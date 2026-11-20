package fr.m2ipm.clinique.jeu;

import fr.m2ipm.clinique.scenario.Orchestrateur;
import fr.m2ipm.clinique.scenario.ScenarioDef;
import fr.m2ipm.clinique.scenario.ScenarioLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Joue une partie complète avec un scénario : une exécution du graphe d'agents = une journée. Après chaque journée, le service
 * relit l'état par MCP ; si l'équipe a oublié {@code fin_de_journee}, le temps passe quand même (le monde n'attend pas).
 * Chaque journée est une exécution visible dans la page /agents.
 */
@Service
public class JoueurService {

    private static final Logger LOG = LoggerFactory.getLogger(JoueurService.class);
    static final int JOURNEES_MAX = 10;

    public record JourJoue(int jour, String runId, boolean finForcee, int scoreApres) {}

    public record Bilan(String scenario, Long graine, int score, int gueris, int morts, int tresorerie, List<JourJoue> jours,
                        long dureeMs) {}

    private final JeuMcp jeu;
    private final ScenarioLoader chargeur;
    private final Orchestrateur orchestrateur;
    private final RestClient http;

    public JoueurService(JeuMcp jeu, ScenarioLoader chargeur, Orchestrateur orchestrateur,
                         @Value("${jeu.url:http://localhost:8090}") String urlJeu) {
        this.jeu = jeu;
        this.chargeur = chargeur;
        this.orchestrateur = orchestrateur;
        this.http = RestClient.builder().baseUrl(urlJeu).build();
    }

    /** @param graine graine du tirage des patients (même graine = mêmes patients : seul l'agent change) ; null = celle du serveur. */
    public Bilan jouer(String idScenario, Long graine, Map<String, String> saisie) {
        ScenarioDef def = chargeur.charger().stream().filter(s -> s.id().equals(idScenario)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Scénario inconnu : " + idScenario));
        if (!jeu.connecte()) {
            throw new IllegalStateException("Le jeu n'est pas connecté : démarrez jeu-bestiaire puis lancez la clinique avec le profil « jeu ».");
        }
        long debut = System.currentTimeMillis();
        http.post().uri(graine == null ? "/api/nouvelle-partie" : "/api/nouvelle-partie?graine=" + graine).retrieve().toBodilessEntity();
        List<JourJoue> jours = new ArrayList<>();
        for (int i = 0; i < JOURNEES_MAX; i++) {
            JeuMcp.Etat avant = jeu.etat();
            if (avant.terminee()) { break; }
            String runId = UUID.randomUUID().toString();
            try {
                orchestrateur.executer(runId, def, saisie == null ? Map.of() : saisie);
            } catch (RuntimeException e) {
                LOG.warn("Journée {} : le scénario a échoué ({}), la journée est terminée d'office.", avant.jour(), e.getMessage());
            }
            JeuMcp.Etat apres = jeu.etat();
            boolean forcee = !apres.terminee() && apres.jour() == avant.jour();
            if (forcee) { jeu.appeler("fin_de_journee", "{}"); }
            jours.add(new JourJoue(avant.jour(), runId, forcee, jeu.etat().score()));
        }
        JeuMcp.Etat fin = jeu.etat();
        return new Bilan(idScenario, graine, fin.score(), fin.gueris(), fin.morts(), fin.tresorerie(), jours,
                System.currentTimeMillis() - debut);
    }
}
