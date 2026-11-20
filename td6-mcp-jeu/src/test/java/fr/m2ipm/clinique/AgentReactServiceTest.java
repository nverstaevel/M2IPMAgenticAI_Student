package fr.m2ipm.clinique;

import static org.assertj.core.api.Assertions.assertThat;

import fr.m2ipm.clinique.agent.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;

/** La boucle est testée sans Spring ni base : modèle scripté, outil factice. */
class AgentReactServiceTest {

    OutilsBidon outils = new OutilsBidon();
    TraceStore traces;

    @BeforeEach
    void init() throws Exception {
        traces = new TraceStore(Files.createTempDirectory("traces"));
    }

    private AgentReactService agent(ModeleScenario m, int budget) {
        return new AgentReactService(m, ToolCallbacks.from(outils), traces, budget);
    }

    @Test
    void reponseFinaleApresUnOutil() {
        var m = new ModeleScenario().appel("Je cherche.", "echo", "{\"texte\":\"a\"}").finale("Voilà.");
        var r = agent(m, 100000).executer("run-1", "demande", 5);
        assertThat(r.motif()).isEqualTo(MotifArret.REPONSE_FINALE);
        assertThat(r.reponse()).isEqualTo("Voilà.");
        assertThat(r.etapes()).hasSize(2);
        assertThat(r.etapes().get(0).outil()).isEqualTo("echo");
        assertThat(r.etapes().get(0).raisonnement()).isEqualTo("Je cherche.");
        assertThat(r.etapes().get(0).resultat()).contains("echo:a");
        assertThat(outils.appels).isEqualTo(1);
        assertThat(r.tokens()).isPositive();
    }

    @Test
    void plafondDEtapes() {
        // le modèle redemande toujours un outil, avec des arguments différents à chaque fois
        var m = new ModeleScenario().appel("1", "echo", "{\"texte\":\"a\"}").appel("2", "echo", "{\"texte\":\"b\"}")
                .appel("3", "echo", "{\"texte\":\"c\"}").appel("4", "echo", "{\"texte\":\"d\"}");
        var r = agent(m, 100000).executer("run-2", "demande", 3);
        assertThat(r.motif()).isEqualTo(MotifArret.PLAFOND_ETAPES);
        assertThat(r.etapes()).hasSize(3);
        assertThat(outils.appels).isEqualTo(3);
        assertThat(m.recus).hasSize(3);
    }

    @Test
    void bouclesDetecteeSurUnMemeAppelDeuxFoisDeSuite() {
        var m = new ModeleScenario().appel("1", "echo", "{\"texte\":\"a\"}").appel("2", "echo", "{\"texte\":\"a\"}");
        var r = agent(m, 100000).executer("run-3", "demande", 10);
        assertThat(r.motif()).isEqualTo(MotifArret.BOUCLE_DETECTEE);
        assertThat(r.etapes()).hasSize(2);
        assertThat(outils.appels).as("le second appel identique n'est pas exécuté").isEqualTo(1);
    }

    @Test
    void budgetDeTokens() {
        var m = new ModeleScenario().appel("1", "echo", "{\"texte\":\"a\"}").appel("2", "echo", "{\"texte\":\"b\"}");
        var r = agent(m, 1).executer("run-4", "demande", 10);
        assertThat(r.motif()).isEqualTo(MotifArret.BUDGET_TOKENS);
        assertThat(r.etapes()).hasSize(1);
    }

    @Test
    void erreurDuModeleEstUnMotifDArret() {
        var m = new ModeleScenario() {
            @Override public org.springframework.ai.chat.model.ChatResponse call(org.springframework.ai.chat.prompt.Prompt p) {
                throw new IllegalStateException("serveur de modèles injoignable");
            }
        };
        var r = agent(m, 100000).executer("run-5", "demande", 3);
        assertThat(r.motif()).isEqualTo(MotifArret.ERREUR);
        assertThat(r.reponse()).contains("injoignable");
    }

    @Test
    void laTraceEstEcriteEnJsonlEtRelue() throws Exception {
        var m = new ModeleScenario().appel("Je cherche.", "long_", "{}").finale("Fin.");
        agent(m, 100000).executer("run-6", "demande", 5);
        List<Etape> relues = traces.lire("run-6");
        assertThat(relues).hasSize(2);
        assertThat(relues.get(0).resultat().length()).isEqualTo(501);   // 500 caractères + « … »
        assertThat(relues.get(1).raisonnement()).isEqualTo("Fin.");
        assertThat(traces.lire("inconnu")).isEmpty();
    }

    @Test
    void lHistoriqueGrandit_etLeContexteDOutilsEstTransmis() {
        var m = new ModeleScenario().appel("1", "echo", "{\"texte\":\"a\"}").finale("ok");
        agent(m, 100000).executer("run-7", "demande", 5);
        assertThat(m.recus.get(1).getInstructions().size()).isGreaterThan(m.recus.get(0).getInstructions().size());
    }
}
