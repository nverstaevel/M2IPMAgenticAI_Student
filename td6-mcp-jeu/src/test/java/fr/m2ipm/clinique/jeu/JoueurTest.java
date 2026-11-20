package fr.m2ipm.clinique.jeu;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import fr.m2ipm.clinique.agent.SuiviExecution;
import fr.m2ipm.clinique.scenario.Agent;
import fr.m2ipm.clinique.scenario.Orchestrateur;
import fr.m2ipm.clinique.scenario.OutilsDisponibles;
import fr.m2ipm.clinique.scenario.Resultat;
import fr.m2ipm.clinique.scenario.ScenarioLoader;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * Le jeu est remplacé par un faux serveur MCP en mémoire (trois jours, +100 par soin) : on vérifie le groupe d'outils « jeu »,
 * la restriction des outils par nœud, et le lanceur de partie (une exécution du graphe = une journée, fin de journée forcée).
 */
class JoueurTest {

    @TempDir Path dossier;
    HttpServer http;
    int jour, gueris;
    final List<String> appels = new ArrayList<>();
    final AtomicInteger reinitialisations = new AtomicInteger();

    @BeforeEach
    void demarrer() throws Exception {
        jour = 1;
        gueris = 0;
        http = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        http.createContext("/api/nouvelle-partie", ex -> {
            reinitialisations.incrementAndGet();
            jour = 1;
            gueris = 0;
            ex.sendResponseHeaders(200, -1);
            ex.close();
        });
        http.start();
    }

    @AfterEach
    void arreter() { http.stop(0); }

    private ToolCallback outil(String nom, java.util.function.Supplier<String> action) {
        return new ToolCallback() {
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(nom).description("faux outil").inputSchema("{\"type\":\"object\"}").build();
            }
            public String call(String entree) { appels.add(nom); return action.get(); }
        };
    }

    private ToolCallback[] fauxJeu() {
        boolean[] termine = {false};
        return new ToolCallback[] {
                outil("etat_clinique", () -> "{\"jour\":" + Math.min(jour, 3) + ",\"pointsAction\":5,\"tresorerie\":500,\"gueris\":" + gueris
                        + ",\"morts\":0,\"terminee\":" + (jour > 3) + ",\"score\":" + (gueris * 100) + "}"),
                outil("soigner", () -> { gueris++; return "{\"ok\":true}"; }),
                outil("fin_de_journee", () -> { jour++; return "{\"ok\":true}"; })};
    }

    private JoueurService service(Map<String, Agent> agents, ToolCallback[] jeu) throws Exception {
        Files.writeString(dossier.resolve("jeu-test.md"), """
                # Test
                ## Agents
                - joueur = a : outils soigner, etat_clinique
                ## Graphe
                entrée -> joueur -> fin
                """);
        var outils = new OutilsDisponibles(Map.of(), () -> jeu);
        var orchestrateur = new Orchestrateur(agents, new SuiviExecution(), outils);
        ObjectProvider<ToolCallbackProvider> fournisseur = fournisseur(jeu);
        return new JoueurService(new JeuMcp(fournisseur), new ScenarioLoader(dossier.toString()), orchestrateur,
                "http://localhost:" + http.getAddress().getPort());
    }

    private static ObjectProvider<ToolCallbackProvider> fournisseur(ToolCallback[] jeu) {
        var f = new DefaultListableBeanFactory();
        f.registerSingleton("mcp", (ToolCallbackProvider) () -> jeu);
        return f.getBeanProvider(ToolCallbackProvider.class);
    }

    @Test
    void leGroupeJeuContientLesOutilsDuServeurEtSeRestreint() {
        ToolCallback[] jeu = fauxJeu();
        var outils = new OutilsDisponibles(Map.of(), () -> jeu);
        assertThat(outils.groupes()).contains("jeu");
        assertThat(outils.outils()).containsExactlyInAnyOrder("etat_clinique", "soigner", "fin_de_journee");
        assertThat(outils.resoudre(List.of("soigner"))).hasSize(1);
        assertThat(outils.resoudre(List.of("jeu"))).hasSize(3);
        assertThat(new OutilsDisponibles(Map.of()).groupes()).doesNotContain("jeu");
    }

    @Test
    void lesChampsDeLEtatSeLisentDansLaReponseMcp() {
        String brut = "[{\"text\":\"{\\\"jour\\\":2,\\\"score\\\":-30,\\\"terminee\\\":true}\"}]";
        assertThat(JeuMcp.entier(brut, "jour")).isEqualTo(2);
        assertThat(JeuMcp.entier(brut, "score")).isEqualTo(-30);
        assertThat(JeuMcp.booleen(brut, "terminee")).isTrue();
        assertThat(JeuMcp.booleen("{\"terminee\":false}", "terminee")).isFalse();
    }

    @Test
    void uneJourneeParExecutionEtFinDeJourneeForceeSiOubliee() throws Exception {
        ToolCallback[] jeu = fauxJeu();
        // L'agent soigne une fois puis rend la main sans terminer la journée.
        Agent distrait = e -> {
            e.outils().stream().filter(o -> o.getToolDefinition().name().equals("soigner")).findFirst().orElseThrow().call("{}");
            return Resultat.texte("fait");
        };
        var bilan = service(Map.of("a", distrait), jeu).jouer("jeu-test", 7L, null);
        assertThat(reinitialisations.get()).isEqualTo(1);
        assertThat(bilan.jours()).hasSize(3);
        assertThat(bilan.jours()).allMatch(JoueurService.JourJoue::finForcee);
        assertThat(bilan.gueris()).isEqualTo(3);
        assertThat(bilan.score()).isEqualTo(300);
    }

    @Test
    void lAgentNeVoitQueLesOutilsAutorisesParLeScenario() throws Exception {
        List<String> vus = new ArrayList<>();
        Agent observateur = e -> {
            e.outils().forEach(o -> vus.add(o.getToolDefinition().name()));
            return Resultat.texte("ok");
        };
        // Le scénario autorise soigner et etat_clinique : fin_de_journee reste invisible, le lanceur la force.
        var bilan = service(Map.of("a", observateur), fauxJeu()).jouer("jeu-test", null, null);
        assertThat(vus).doesNotContain("fin_de_journee").contains("soigner", "etat_clinique");
        assertThat(bilan.jours()).allMatch(JoueurService.JourJoue::finForcee);
        assertThat(bilan.jours()).hasSize(3);
    }

    @Test
    void sansJeuLeLanceurRefuse() throws Exception {
        var s = service(Map.of("a", e -> Resultat.texte("x")), new ToolCallback[0]);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> s.jouer("jeu-test", null, null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("jeu");
    }
}
