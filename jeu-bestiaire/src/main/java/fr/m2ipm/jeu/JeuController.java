package fr.m2ipm.jeu;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Hors MCP : la page de jeu (/), la remise à zéro et les actions « à la main », réservées à l'humain qui observe ou joue. */
@RestController
@RequestMapping("/api")
public class JeuController {

    private final PartieService parties;

    public JeuController(PartieService parties) { this.parties = parties; }

    @GetMapping("/partie")
    public Map<String, Object> partie() {
        Jeu j = parties.jeu();
        return Map.of("etat", j.etat(), "patients", j.patients(), "pharmacie", j.pharmacie(), "journal", j.journal(), "carnet", j.carnet());
    }

    @PostMapping("/nouvelle-partie")
    public Jeu.Etat nouvelle(@RequestParam(name = "graine", required = false) Long graine) { return parties.nouvelle(graine).etat(); }

    // ---- jeu « à la main » : réservé à l'humain qui joue depuis la page ; un agent, lui, n'a que les outils MCP ----

    @PostMapping("/examiner")
    public Jeu.Compte examiner(@RequestParam("patient") String patient) { return parties.jeu().examiner(patient); }

    @PostMapping("/soigner")
    public Jeu.Compte soigner(@RequestParam("patient") String patient, @RequestParam("medicament") String medicament) {
        return parties.jeu().soigner(patient, medicament);
    }

    @PostMapping("/commander")
    public Jeu.Compte commander(@RequestParam("medicament") String medicament, @RequestParam(name = "quantite", defaultValue = "1") int quantite) {
        return parties.jeu().commander(medicament, quantite);
    }

    @PostMapping("/fin-de-journee")
    public Jeu.Compte finDeJournee() { return parties.jeu().finDeJournee(); }

    @GetMapping("/carnet")
    public List<Jeu.Observation> carnet() { return parties.jeu().carnet(); }

    @GetMapping("/journal")
    public List<String> journal() { return parties.jeu().journal(); }
}
