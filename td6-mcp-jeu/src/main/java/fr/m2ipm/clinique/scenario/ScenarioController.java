package fr.m2ipm.clinique.scenario;

import fr.m2ipm.clinique.agent.Evenement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Scénarios lus dans des fichiers .md : liste, aperçu du graphe prévu, lancement. Réservé au rôle PERSONNEL
 * (/api/agent/** dans SecurityConfig) : les exécutions manipulent des données de dossiers.
 */
@RestController
@RequestMapping("/api/agent/scenarios")
public class ScenarioController {

    public record Resume(String id, String titre, String forme, String fichier, boolean valide, List<String> erreurs,
                         List<ChampEntree> entree) {}

    public record Detail(Resume resume, String description, List<Evenement> apercu, List<String> agentsDisponibles,
                         List<String> groupesDOutils, List<String> outilsDisponibles) {}

    private final ScenarioLoader chargeur;
    private final Orchestrateur orchestrateur;

    public ScenarioController(ScenarioLoader chargeur, Orchestrateur orchestrateur) {
        this.chargeur = chargeur;
        this.orchestrateur = orchestrateur;
    }

    @GetMapping
    public List<Resume> lister() {
        return chargeur.charger().stream().map(this::resume).toList();
    }

    @GetMapping("/{id}")
    public Detail detail(@PathVariable String id) {
        ScenarioDef d = trouver(id);
        List<String> erreurs = orchestrateur.valider(d);
        List<Evenement> apercu = erreurs.isEmpty() ? orchestrateur.apercu(d) : List.of();
        return new Detail(resume(d), d.description(), apercu, orchestrateur.agentsDisponibles(),
                orchestrateur.outilsDisponibles().groupes(), orchestrateur.outilsDisponibles().outils());
    }

    @PostMapping("/{id}/lancer")
    public Map<String, String> lancer(@PathVariable String id, @RequestBody(required = false) Map<String, Object> saisie) {
        ScenarioDef d = trouver(id);
        Map<String, String> donnees = new LinkedHashMap<>();
        if (saisie != null) { saisie.forEach((k, v) -> donnees.put(k, v == null ? "" : String.valueOf(v))); }
        try {
            return Map.of("runId", orchestrateur.lancer(d, donnees));
        } catch (ScenarioException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private Resume resume(ScenarioDef d) {
        List<String> erreurs = orchestrateur.valider(d);
        return new Resume(d.id(), d.titre(), orchestrateur.forme(d), d.fichier(), erreurs.isEmpty(), erreurs, d.entree());
    }

    private ScenarioDef trouver(String id) {
        return chargeur.charger().stream().filter(s -> s.id().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scénario inconnu : " + id));
    }
}
