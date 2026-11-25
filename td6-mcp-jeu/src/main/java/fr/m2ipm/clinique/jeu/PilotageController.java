package fr.m2ipm.clinique.jeu;

import fr.m2ipm.clinique.scenario.ScenarioLoader;
import fr.m2ipm.clinique.scenario.Orchestrateur;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

/**
 * La page {@code /pilotage} (lancer une partie, suivre les actions et les réflexions de l'agent en direct) et son API, sous
 * {@code /api/agent/**} : réservée au rôle PERSONNEL comme le reste du pilotage des agents.
 */
@Controller
public class PilotageController {

    private final PilotageService pilotage;
    private final JeuMcp jeu;
    private final ScenarioLoader chargeur;
    private final Orchestrateur orchestrateur;
    private final Environment env;
    private final String urlJeu;

    public PilotageController(PilotageService pilotage, JeuMcp jeu, ScenarioLoader chargeur, Orchestrateur orchestrateur,
                              Environment env, @Value("${jeu.url:http://localhost:8090}") String urlJeu) {
        this.pilotage = pilotage;
        this.jeu = jeu;
        this.chargeur = chargeur;
        this.orchestrateur = orchestrateur;
        this.env = env;
        this.urlJeu = urlJeu;
    }

    @GetMapping("/pilotage")
    public String page() { return "pilotage"; }

    /** Ce que la page propose : joueurs disponibles, connexion au jeu, profil du modèle actif. */
    @GetMapping("/api/agent/jeu/pilotage/options")
    @ResponseBody
    public Map<String, Object> options() {
        List<Map<String, Object>> joueurs = new ArrayList<>();
        joueurs.add(Map.of("id", JoueurRegles.ID, "titre", "Règles fixes (sans modèle)", "valide", true));
        chargeur.charger().stream().sorted(java.util.Comparator.comparing(s -> s.id())).forEach(s -> {
            List<String> erreurs = orchestrateur.valider(s);
            joueurs.add(Map.of("id", s.id(), "titre", s.titre() == null ? s.id() : s.titre(), "valide", erreurs.isEmpty(),
                    "erreurs", erreurs));
        });
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("connecte", jeu.connecte());
        r.put("urlJeu", urlJeu);
        r.put("profils", String.join(",", env.getActiveProfiles()));
        r.put("joueurs", joueurs);
        return r;
    }

    /** L'état de la partie pilotée et, si le jeu répond, l'état du jeu lui-même. */
    @GetMapping("/api/agent/jeu/pilotage/etat")
    @ResponseBody
    public Map<String, Object> etat() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("partie", pilotage.instantane());
        JeuMcp.Etat e = null;
        try { if (jeu.connecte()) { e = jeu.etat(); } } catch (RuntimeException ignore) { /* le jeu s'arrête ou redémarre */ }
        r.put("jeu", e);
        return r;
    }

    @PostMapping("/api/agent/jeu/pilotage/demarrer")
    @ResponseBody
    public Map<String, Object> demarrer(@RequestParam("scenario") String scenario,
                                        @RequestParam(name = "graine", required = false) Long graine,
                                        @RequestParam(name = "pauseMs", required = false) Long pauseMs) {
        try {
            pilotage.demarrer(scenario, graine, pauseMs);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
        return Map.of("demarree", true);
    }

    /** Règle la pause entre deux actions (ms), y compris pendant une partie. */
    @PostMapping("/api/agent/jeu/pilotage/pause")
    @ResponseBody
    public Map<String, Object> pause(@RequestParam("ms") long ms) {
        pilotage.regler(ms);
        return Map.of("pauseMs", ms);
    }

    @PostMapping("/api/agent/jeu/pilotage/arreter")
    @ResponseBody
    public Map<String, Object> arreter() {
        pilotage.arreter();
        return Map.of("arretDemande", true);
    }
}
