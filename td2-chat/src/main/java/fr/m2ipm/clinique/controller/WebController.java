package fr.m2ipm.clinique.controller;

import fr.m2ipm.clinique.dto.CreatureDto;
import fr.m2ipm.clinique.service.CliniqueService;
import fr.m2ipm.clinique.service.ConflitDeCreneauException;
import fr.m2ipm.clinique.service.RessourceIntrouvableException;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** Pages Thymeleaf : recherche, fiche propriétaire, formulaire de consultation. */
@Controller
public class WebController {

    private final CliniqueService service;

    public WebController(CliniqueService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String accueil() {
        return "redirect:/proprietaires";
    }

    @GetMapping("/chat")
    public String chat() {
        return "chat";
    }

    @GetMapping("/proprietaires")
    public String recherche(@RequestParam(defaultValue = "") String nom, Model model) {
        model.addAttribute("nom", nom);
        model.addAttribute("proprietaires", service.rechercherProprietaires(nom));
        return "proprietaires";
    }

    @GetMapping("/proprietaires/{id}")
    public String fiche(@PathVariable Long id, Model model) {
        model.addAttribute("proprietaire", service.trouverProprietaire(id));
        model.addAttribute("creatures", service.creaturesDuProprietaire(id));
        // Historique de chaque créature, indexé par identifiant de créature.
        var historiques = new java.util.LinkedHashMap<Long, Object>();
        service.creaturesDuProprietaire(id)
                .forEach(c -> historiques.put(c.id(), service.consultationsDeLaCreature(c.id())));
        model.addAttribute("historiques", historiques);
        return "proprietaire";
    }

    @GetMapping("/consultations/nouvelle")
    public String formulaire(@RequestParam Long creatureId, Model model) {
        preparerFormulaire(creatureId, model);
        return "consultation-form";
    }

    @PostMapping("/consultations")
    public String planifier(@RequestParam Long creatureId, @RequestParam Long soigneurId,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateHeure,
                            @RequestParam String motif, Model model) {
        try {
            service.planifierConsultation(creatureId, soigneurId, dateHeure, motif);
        } catch (ConflitDeCreneauException e) {
            model.addAttribute("erreur", e.getMessage());
            preparerFormulaire(creatureId, model);
            return "consultation-form";
        }
        return "redirect:/proprietaires/" + service.trouverCreature(creatureId).proprietaireId();
    }

    @ExceptionHandler(RessourceIntrouvableException.class)
    public String introuvable(RessourceIntrouvableException e, Model model) {
        model.addAttribute("message", e.getMessage());
        return "erreur";
    }

    private void preparerFormulaire(Long creatureId, Model model) {
        CreatureDto creature = service.trouverCreature(creatureId);
        model.addAttribute("creature", creature);
        model.addAttribute("soigneurs", service.soigneursPourEspece(creature.espece()));
    }
}
