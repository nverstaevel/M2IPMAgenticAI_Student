package fr.m2ipm.clinique;

import static org.assertj.core.api.Assertions.assertThat;

import fr.m2ipm.clinique.agent.Evenement;
import fr.m2ipm.clinique.agent.SuiviExecution;
import fr.m2ipm.clinique.roles.*;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

/** Le graphe du duo : tours du vétérinaire et du gardien, puis validation humaine en attente. */
class SuiviRolesTest {

    static final CasBestiaire CAS = new CasBestiaire(1, "Lunatique", "Loup-garou", "Clairdelune", "insomnie",
            "compléments à base d'argent", Verdict.REJETE);

    @Test
    void leDuoProduitUnGrapheAvecValidationHumaine() {
        var suivi = new SuiviExecution();
        var duo = new DuoService(new VeterinaireAgent(ChatClient.create(new ModeleTexte(DuoServiceTest.DIAG.formatted("argent")))),
                new GardienDuBestiaire(ChatClient.create(new ModeleTexte(DuoServiceTest.REJETE, DuoServiceTest.APPROUVE))), Clock.systemUTC(), "test", suivi);
        var run = duo.traiter("run-duo-1", CAS, ProfilActeur.PRESSE);
        var evts = suivi.evenements("run-duo-1").orElseThrow();
        assertThat(evts).extracting(Evenement::id).contains("cas", "v1", "g1", "humain", "fin");
        assertThat(evts.stream().filter(e -> "g1".equals(e.id())).reduce((a, b) -> b).orElseThrow().statut()).isEqualTo(Evenement.ERREUR);
        assertThat(evts.stream().filter(e -> "humain".equals(e.id())).findFirst().orElseThrow().statut()).isEqualTo(Evenement.ATTENTE);
        assertThat(run.tours()).isNotEmpty();
    }

    /** Un travailleur dont l'appel échoue deux fois (appel d'outil rejeté par le fournisseur) ne doit pas faire échouer tout le plan. */
    @Test
    void leSuperviseurPoursuitAvecUnResultatPartielSiUnTravailleurEchoue() {
        var suivi = new SuiviExecution();
        var plan = "{\"sousTaches\":[{\"travailleur\":\"DOSSIER\",\"consigne\":\"dossier\"},{\"travailleur\":\"PHARMACIE\",\"consigne\":\"stock\"}]}";
        var superviseur = ChatClient.create(new ModeleTexte(plan, "Synthèse partielle."));
        var casse = new org.springframework.ai.chat.model.ChatModel() {
            @Override public org.springframework.ai.chat.model.ChatResponse call(org.springframework.ai.chat.prompt.Prompt p) {
                throw new IllegalStateException("400: tool call validation failed");
            }
        };
        var travailleurs = new java.util.EnumMap<Plan.Travailleur, ChatClient>(Plan.Travailleur.class);
        travailleurs.put(Plan.Travailleur.DOSSIER, ChatClient.create(casse));
        travailleurs.put(Plan.Travailleur.PHARMACIE, ChatClient.create(new ModeleTexte("Stock : 12.")));
        var agent = new SuperviseurAgent(superviseur, travailleurs, new fr.m2ipm.clinique.agent.TraceStore(
                java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "traces-test-sup")), suivi);
        var r = agent.executer("run-sup-1", "demande");
        assertThat(r.synthese()).isEqualTo("Synthèse partielle.");
        var evts = suivi.evenements("run-sup-1").orElseThrow();
        assertThat(evts.stream().filter(e -> "t1".equals(e.id())).reduce((a, b) -> b).orElseThrow().statut()).isEqualTo(Evenement.ERREUR);
        assertThat(evts.stream().filter(e -> "t2".equals(e.id())).reduce((a, b) -> b).orElseThrow().statut()).isEqualTo(Evenement.OK);
        assertThat(evts.stream().filter(e -> "fin".equals(e.id())).findFirst().orElseThrow().statut()).isEqualTo(Evenement.OK);
    }

    /** Chaque appel au modèle est compté (entrée, sortie) et cumulé sur le nœud de l'agent qui l'a fait. */
    @Test
    void lesJetonsSontCumulesParNoeud() {
        var suivi = new SuiviExecution();
        java.util.function.Function<String, org.springframework.ai.chat.model.ChatModel> avecUsage = texte -> new org.springframework.ai.chat.model.ChatModel() {
            @Override public org.springframework.ai.chat.model.ChatResponse call(org.springframework.ai.chat.prompt.Prompt p) {
                return new org.springframework.ai.chat.model.ChatResponse(
                        java.util.List.of(new org.springframework.ai.chat.model.Generation(new org.springframework.ai.chat.messages.AssistantMessage(texte))),
                        org.springframework.ai.chat.metadata.ChatResponseMetadata.builder()
                                .usage(new org.springframework.ai.chat.metadata.DefaultUsage(100, 20)).build());
            }
        };
        var vet = ChatClient.builder(avecUsage.apply(DuoServiceTest.DIAG.formatted("tisane")))
                .defaultAdvisors(fr.m2ipm.clinique.agent.ComptageJetons.advisor()).build();
        var gar = ChatClient.builder(avecUsage.apply(DuoServiceTest.APPROUVE))
                .defaultAdvisors(fr.m2ipm.clinique.agent.ComptageJetons.advisor()).build();
        new DuoService(new VeterinaireAgent(vet), new GardienDuBestiaire(gar), Clock.systemUTC(), "test", suivi)
                .traiter("run-jetons", CAS, ProfilActeur.PRUDENT);
        var evts = suivi.evenements("run-jetons").orElseThrow();
        var v1 = evts.stream().filter(e -> "v1".equals(e.id()) && e.tokens() != null).reduce((a, b) -> b).orElseThrow();
        assertThat(v1.tokens()).isEqualTo(120);
        assertThat(v1.tokensEntree()).isEqualTo(100);
        assertThat(v1.tokensSortie()).isEqualTo(20);
        assertThat(v1.appelsModele()).isEqualTo(1);
        assertThat(v1.tokensEstimes()).isFalse();
        var g1 = evts.stream().filter(e -> "g1".equals(e.id()) && e.tokens() != null).reduce((a, b) -> b).orElseThrow();
        assertThat(g1.tokens()).isEqualTo(120);
    }
}
