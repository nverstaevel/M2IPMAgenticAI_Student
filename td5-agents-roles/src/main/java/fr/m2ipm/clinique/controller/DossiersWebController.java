package fr.m2ipm.clinique.controller;

import fr.m2ipm.clinique.roles.DossierRun;
import fr.m2ipm.clinique.roles.DuoService;
import java.security.Principal;
import java.time.Instant;
import java.util.Comparator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** Page du personnel : liste des dossiers et bouton Valider / Refuser. */
@Controller
public class DossiersWebController {

    private final DuoService duo;

    public DossiersWebController(DuoService duo) { this.duo = duo; }

    @GetMapping("/dossiers")
    public String liste(Model model) {
        model.addAttribute("dossiers", duo.runs().stream().sorted(Comparator.comparing(DossierRun::date).reversed()).toList());
        return "dossiers";
    }

    @PostMapping("/dossiers/{runId}/decision")
    public String decider(@PathVariable String runId, @RequestParam boolean valide, Principal agent) {
        duo.run(runId).ifPresent(r -> {
            try { r.decider(valide, agent.getName(), Instant.now()); } catch (IllegalStateException ignore) { /* déjà décidé */ }
        });
        return "redirect:/dossiers";
    }
}
