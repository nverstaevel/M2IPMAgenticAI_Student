package fr.m2ipm.jeu;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Hors MCP : le tableau de bord (page /) et la remise à zéro, réservés à l'humain qui observe la partie. */
@RestController
@RequestMapping("/api")
public class JeuController {

    private final PartieService parties;

    public JeuController(PartieService parties) { this.parties = parties; }

    @GetMapping("/partie")
    public Map<String, Object> partie() {
        Jeu j = parties.jeu();
        return Map.of("etat", j.etat(), "patients", j.patients(), "pharmacie", j.pharmacie(), "journal", j.journal());
    }

    @PostMapping("/nouvelle-partie")
    public Jeu.Etat nouvelle(@RequestParam(required = false) Long graine) { return parties.nouvelle(graine).etat(); }

    @GetMapping("/journal")
    public List<String> journal() { return parties.jeu().journal(); }
}
