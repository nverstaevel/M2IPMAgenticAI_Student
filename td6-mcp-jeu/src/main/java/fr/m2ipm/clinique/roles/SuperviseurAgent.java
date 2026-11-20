package fr.m2ipm.clinique.roles;

import fr.m2ipm.clinique.agent.Etape;
import fr.m2ipm.clinique.agent.ComptageJetons;
import fr.m2ipm.clinique.agent.Evenement;
import fr.m2ipm.clinique.agent.SuiviExecution;
import fr.m2ipm.clinique.agent.SuiviOutils;
import org.springframework.ai.support.ToolCallbacks;
import fr.m2ipm.clinique.agent.TraceStore;
import fr.m2ipm.clinique.ai.OutilsClinique;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Orchestrateur-workers minimal : le superviseur planifie (sortie structurée, aucun outil), confie chaque sous-tâche
 * à un travailleur qui n'a que ses outils, puis assemble. Chaque appel de travailleur est une étape de la trace.
 * Le superviseur n'a aucun outil : il ne peut pas écrire dans l'agenda.
 */
@Component
public class SuperviseurAgent {

    static final String SYSTEME_SUPERVISEUR = """
            [ROLE:SUPERVISEUR]
            Tu supervises trois travailleurs : DOSSIER (dossier des créatures et des propriétaires), PHARMACIE (médicaments, stocks, tarifs)
            et AGENDA (créneaux libres, proposition d'événement). Pour la demande reçue, produis un plan : une liste ordonnée de
            sous-tâches, chacune avec le travailleur et une consigne autonome. Tu ne fais rien toi-même.
            """;

    public record Resultat(String runId, Plan plan, List<Etape> etapes, String synthese) {}

    private final ChatClient superviseur;
    private final Map<Plan.Travailleur, ChatClient> travailleurs = new EnumMap<>(Plan.Travailleur.class);
    private final TraceStore traces;
    private final SuiviExecution suivi;

    /** Chaque client part d'un clone du builder : sinon les prompts et outils des uns fuiteraient chez les autres. */
    @Autowired
    public SuperviseurAgent(ChatClient.Builder builder, OutilsClinique dossier, OutilsPharmacie pharmacie,
                            OutilsAgenda agenda, TraceStore traces, SuiviExecution suivi) {
        this(builder.clone().defaultSystem(SYSTEME_SUPERVISEUR).defaultAdvisors(ComptageJetons.advisor()).build(),
                Map.of(Plan.Travailleur.DOSSIER, builder.clone().defaultSystem("Tu es DossierWorker : tu réponds avec tes outils, sans rien inventer.").defaultToolCallbacks(SuiviOutils.envelopper(ToolCallbacks.from(dossier))).defaultAdvisors(ComptageJetons.advisor()).build(),
                        Plan.Travailleur.PHARMACIE, builder.clone().defaultSystem("Tu es PharmacieWorker : tu réponds avec tes outils, sans rien inventer.").defaultToolCallbacks(SuiviOutils.envelopper(ToolCallbacks.from(pharmacie))).defaultAdvisors(ComptageJetons.advisor()).build(),
                        Plan.Travailleur.AGENDA, builder.clone().defaultSystem("Tu es AgendaWorker : tu réponds avec tes outils ; tu ne fais que PROPOSER un événement.").defaultToolCallbacks(SuiviOutils.envelopper(ToolCallbacks.from(agenda))).defaultAdvisors(ComptageJetons.advisor()).build()),
                traces, suivi);
    }

    public SuperviseurAgent(ChatClient superviseur, Map<Plan.Travailleur, ChatClient> travailleurs, TraceStore traces) {
        this(superviseur, travailleurs, traces, new SuiviExecution());
    }

    public SuperviseurAgent(ChatClient superviseur, Map<Plan.Travailleur, ChatClient> travailleurs, TraceStore traces,
                            SuiviExecution suivi) {
        this.superviseur = superviseur;
        this.travailleurs.putAll(travailleurs);
        this.traces = traces;
        this.suivi = suivi;
    }

    public Resultat executer(String demande) {
        return executer(UUID.randomUUID().toString(), demande);
    }

    /** Le runId est choisi par l'appelant pour que la page /agents suive l'exécution avant sa fin. */
    public Resultat executer(String runId, String demande) {
        suivi.demarrer(runId, "Superviseur : " + abreger(demande, 60));
        suivi.noeud(runId, "demande", List.of(), Evenement.UTILISATEUR, "Demande", Evenement.OK, demande, null, null, null);
        String enCours = "plan";
        try {
            suivi.noeud(runId, "plan", List.of("demande"), Evenement.AGENT, "Superviseur · plan", Evenement.EN_COURS,
                    "Planifie les sous-tâches (sortie structurée, aucun outil)", null, null, null);
            long t0 = System.nanoTime();
            Plan plan;
            try (var p = SuiviOutils.ouvrir(suivi, runId, "plan")) {
                plan = SuiviOutils.reessayer(suivi, runId, "plan", () -> superviseur.prompt().user(demande).call().entity(Plan.class));
            }
            suivi.maj(runId, "plan", Evenement.OK, null,
                    plan.sousTaches().stream().map(t -> t.travailleur() + " : " + t.consigne()).reduce((a, b) -> a + "\n" + b).orElse("(vide)"),
                    (System.nanoTime() - t0) / 1_000_000, null);

            List<Etape> etapes = new ArrayList<>();
            StringBuilder reponses = new StringBuilder();
            List<String> termines = new ArrayList<>();
            int numero = 0;
            for (Plan.SousTache t : plan.sousTaches()) {
                numero++;
                String id = "t" + numero;
                enCours = id;
                suivi.noeud(runId, id, List.of("plan"), Evenement.TRAVAILLEUR, t.travailleur() + "Worker", Evenement.EN_COURS,
                        t.consigne(), null, null, null);
                long debut = System.nanoTime();
                String reponse = null;
                int jetonsTravailleur = 0;
                RuntimeException echec = null;
                for (int tentative = 1; tentative <= 2 && reponse == null; tentative++) {
                    try (var p = SuiviOutils.ouvrir(suivi, runId, id)) {
                        reponse = travailleurs.get(t.travailleur()).prompt().user(t.consigne())
                                .toolContext(Map.of("conversationId", runId)).call().content();
                        jetonsTravailleur = p.total();
                    } catch (RuntimeException e) {
                        // Appel d'outil mal formé par le modèle (rejeté par le fournisseur) : une seconde tentative suffit souvent.
                        echec = e;
                        suivi.maj(runId, id, Evenement.EN_COURS, "Tentative " + tentative + " échouée : " + e.getMessage()
                                + (tentative < 2 ? "\nNouvelle tentative…" : ""), null, null, null);
                    }
                }
                if (reponse == null) {
                    // Le travailleur a échoué deux fois : on poursuit avec un résultat partiel plutôt que d'abandonner tout le plan.
                    reponse = "(échec du travailleur " + t.travailleur() + " : " + echec.getMessage() + ")";
                    suivi.maj(runId, id, Evenement.ERREUR, null, reponse, (System.nanoTime() - debut) / 1_000_000, null);
                    reponses.append(t.travailleur()).append(" : ").append(reponse).append('\n');
                    termines.add(id);
                    continue;
                }
                String r = reponse == null ? "" : reponse;
                long ms = (System.nanoTime() - debut) / 1_000_000;
                var etape = new Etape(numero, "Délégation à " + t.travailleur() + "Worker", t.travailleur() + "Worker",
                        t.consigne(), r.length() > 500 ? r.substring(0, 500) + "…" : r, ms,
                        jetonsTravailleur > 0 ? jetonsTravailleur : (t.consigne().length() + r.length()) / 4);
                etapes.add(etape);
                traces.ajouter(runId, etape);
                suivi.maj(runId, id, Evenement.OK, null, abreger(r, 800), ms, etape.tokens());
                termines.add(id);
                reponses.append(t.travailleur()).append(" : ").append(r).append('\n');
            }
            enCours = "synthese";
            suivi.noeud(runId, "synthese", termines.isEmpty() ? List.of("plan") : termines, Evenement.AGENT, "Superviseur · synthèse",
                    Evenement.EN_COURS, "Assemble les réponses des travailleurs", null, null, null);
            long t1 = System.nanoTime();
            String promptSynthese = "Demande : " + demande + "\nRéponses des travailleurs :\n" + reponses
                    + "Rédige la réponse finale pour l'utilisateur, sans inventer d'information.";
            String synthese;
            try (var p = SuiviOutils.ouvrir(suivi, runId, "synthese")) {
                synthese = SuiviOutils.reessayer(suivi, runId, "synthese", () -> superviseur.prompt().user(promptSynthese).call().content());
            }
            suivi.maj(runId, "synthese", Evenement.OK, null, abreger(synthese, 800), (System.nanoTime() - t1) / 1_000_000, null);
            suivi.noeud(runId, "fin", List.of("synthese"), Evenement.FIN, "Réponse finale", Evenement.OK, null,
                    synthese, null, null);
            suivi.terminer(runId, "REPONSE_FINALE");
            return new Resultat(runId, plan, etapes, synthese);
        } catch (RuntimeException e) {
            suivi.maj(runId, enCours, Evenement.ERREUR, null, String.valueOf(e.getMessage()), null, null);
            suivi.noeud(runId, "fin", List.of(enCours), Evenement.FIN, "Erreur", Evenement.ERREUR, String.valueOf(e.getMessage()),
                    null, null, null);
            suivi.terminer(runId, "ERREUR");
            throw e;
        }
    }

    private static String abreger(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
