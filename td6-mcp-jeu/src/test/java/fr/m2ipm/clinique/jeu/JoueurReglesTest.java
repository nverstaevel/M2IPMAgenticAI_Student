package fr.m2ipm.clinique.jeu;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import fr.m2ipm.clinique.agent.SuiviExecution;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * Le joueur à règles contre un faux jeu en mémoire (3 jours, 2 patients vampires dont le remède n'est en stock qu'une fois par
 * jour) : il examine, soigne le plus fragile, commande pour l'autre, termine chaque journée, et lit les réponses MCP emballées.
 */
class JoueurReglesTest {

    HttpServer http;
    int jour;
    final List<String> appels = new ArrayList<>();

    @BeforeEach
    void demarrer() throws Exception {
        jour = 1;
        http = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        http.createContext("/api/nouvelle-partie", ex -> { jour = 1; ex.sendResponseHeaders(200, -1); ex.close(); });
        http.start();
    }

    @AfterEach
    void arreter() { http.stop(0); }

    private ToolCallback outil(String nom, Supplier<String> reponse) {
        return new ToolCallback() {
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(nom).description("faux outil").inputSchema("{\"type\":\"object\"}").build();
            }
            public String call(String entree) { appels.add(nom + entree); return reponse.get(); }
        };
    }

    private ToolCallback[] fauxJeu() {
        String ok = "{\"ok\":true,\"message\":\"fait\"}";
        return new ToolCallback[] {
                outil("etat_clinique", () -> "{\"jour\":" + Math.min(jour, 3) + ",\"pointsAction\":5,\"tresorerie\":500,\"gueris\":0,\"morts\":0,\"terminee\":"
                        + (jour > 3) + ",\"score\":0}"),
                // Réponse emballée comme le fait parfois MCP : une liste d'un contenu texte.
                outil("lister_patients", () -> "[{\"text\":\"[{\\\"id\\\":\\\"P02\\\",\\\"espece\\\":\\\"vampire\\\",\\\"pv\\\":80,\\\"signeDistinctif\\\":\\\"crocs ternes\\\"},"
                        + "{\\\"id\\\":\\\"P01\\\",\\\"espece\\\":\\\"vampire\\\",\\\"pv\\\":40,\\\"signeDistinctif\\\":\\\"crocs ternes\\\"}]\"}]"),
                outil("fiche_espece", () -> "{\"espece\":\"vampire\",\"maladies\":[{\"nom\":\"Soif\",\"signeDistinctif\":\"crocs ternes\",\"remede\":\"sang-de-synthese\"},"
                        + "{\"nom\":\"Ombre\",\"signeDistinctif\":\"reflet absent\",\"remede\":\"aconit-dilue\"}]}"),
                outil("consulter_pharmacie", () -> "[{\"medicament\":\"sang-de-synthese\",\"stock\":1},{\"medicament\":\"aconit-dilue\",\"stock\":2}]"),
                outil("examiner_patient", () -> ok),
                outil("soigner", () -> ok),
                outil("commander", () -> ok),
                outil("fin_de_journee", () -> { jour++; return ok; })};
    }

    private JoueurRegles regles() {
        ToolCallback[] jeu = fauxJeu();
        var f = new DefaultListableBeanFactory();
        f.registerSingleton("mcp", (ToolCallbackProvider) () -> jeu);
        ObjectProvider<ToolCallbackProvider> fournisseur = f.getBeanProvider(ToolCallbackProvider.class);
        return new JoueurRegles(new JeuMcp(fournisseur), new SuiviExecution(), "http://localhost:" + http.getAddress().getPort());
    }

    @Test
    void lesReglesSoignentLePlusFragilePuisCommandentPourLAutre() {
        var jours = new ArrayList<JoueurService.JourJoue>();
        var bilan = regles().jouer(7L, new JoueurService.Ecoute() {
            public void jourTermine(JoueurService.JourJoue j) { jours.add(j); }
        });
        assertThat(bilan.scenario()).isEqualTo(JoueurRegles.ID);
        assertThat(bilan.jours()).hasSize(3);
        assertThat(jours).hasSize(3);
        assertThat(appels.stream().filter(a -> a.startsWith("soigner")).toList()).hasSize(3)
                .allMatch(a -> a.contains("P01") && a.contains("sang-de-synthese"));
        assertThat(appels.stream().filter(a -> a.startsWith("commander")).toList()).hasSize(3)
                .allMatch(a -> a.contains("sang-de-synthese"));
        assertThat(appels.stream().filter(a -> a.startsWith("fin_de_journee")).count()).isEqualTo(3);
        assertThat(bilan.jours()).noneMatch(JoueurService.JourJoue::finForcee);
    }

    @Test
    void lArretDemandeEstRespecteEntreDeuxJournees() {
        var bilan = regles().jouer(null, new JoueurService.Ecoute() {
            public boolean arretDemande() { return jour > 1; }
        });
        assertThat(bilan.jours()).hasSize(1);
    }

    @Test
    void lesReponsesEmballeesSeDeballent() {
        var r = regles();
        assertThat(r.deballer("{\"ok\":true}")).isInstanceOf(java.util.Map.class);
        assertThat(r.deballer("[{\"text\":\"{\\\"jour\\\":2}\"}]")).isInstanceOf(java.util.Map.class);
        assertThat(r.deballer("\"[1,2]\"")).isInstanceOf(java.util.List.class);
    }
}
