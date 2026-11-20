package fr.m2ipm.clinique.controller;

import fr.m2ipm.clinique.dto.*;
import fr.m2ipm.clinique.service.CliniqueService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final CliniqueService service;

    public ApiController(CliniqueService service) {
        this.service = service;
    }

    @GetMapping("/proprietaires")
    public List<ProprietaireDto> rechercher(@RequestParam(defaultValue = "") String nom) {
        return service.rechercherProprietaires(nom);
    }

    @GetMapping("/proprietaires/{id}")
    public ProprietaireDto proprietaire(@PathVariable Long id) {
        return service.trouverProprietaire(id);
    }

    @GetMapping("/proprietaires/{id}/creatures")
    public List<CreatureDto> creatures(@PathVariable Long id) {
        return service.creaturesDuProprietaire(id);
    }

    @GetMapping("/creatures/{id}/consultations")
    public List<ConsultationDto> consultations(@PathVariable Long id) {
        return service.consultationsDeLaCreature(id);
    }

    @GetMapping("/soigneurs")
    public List<SoigneurDto> soigneurs(@RequestParam String espece) {
        return service.soigneursPourEspece(espece);
    }

    @PostMapping("/consultations")
    public ResponseEntity<ConsultationDto> planifier(@Valid @RequestBody PlanificationRequest requete) {
        ConsultationDto c = service.planifierConsultation(
                requete.creatureId(), requete.soigneurId(), requete.dateHeure(), requete.motif());
        return ResponseEntity.created(URI.create("/api/creatures/" + c.creatureId() + "/consultations")).body(c);
    }

    @PostMapping("/consultations/{id}/diagnostic")
    public ResponseEntity<Void> diagnostic(@PathVariable Long id, @Valid @RequestBody DiagnosticRequest requete) {
        service.enregistrerDiagnostic(id, requete.diagnostic(), requete.traitement());
        return ResponseEntity.noContent().build();
    }
}
