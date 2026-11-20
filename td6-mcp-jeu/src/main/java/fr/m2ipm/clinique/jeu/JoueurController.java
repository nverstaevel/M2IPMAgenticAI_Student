package fr.m2ipm.clinique.jeu;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Lance une partie complète avec un scénario (réservé au rôle PERSONNEL : /api/agent/** dans SecurityConfig). */
@RestController
@RequestMapping("/api/agent/jeu")
public class JoueurController {

    private final JoueurService joueur;
    private final JeuMcp jeu;

    public JoueurController(JoueurService joueur, JeuMcp jeu) {
        this.joueur = joueur;
        this.jeu = jeu;
    }

    /** Le jeu est-il connecté, et que publie-t-il ? Premier réflexe en cas de problème. */
    @GetMapping("/outils")
    public Map<String, Object> outils() {
        var noms = java.util.Arrays.stream(jeu.outils()).map(o -> o.getToolDefinition().name()).toList();
        return Map.of("connecte", !noms.isEmpty(), "outils", noms);
    }

    /** Exemple : {@code POST /api/agent/jeu/partie?scenario=jeu-equipe&graine=42}. Dure plusieurs minutes avec un vrai modèle. */
    @PostMapping("/partie")
    public JoueurService.Bilan partie(@RequestParam String scenario, @RequestParam(required = false) Long graine) {
        try {
            return joueur.jouer(scenario, graine, null);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }
}
