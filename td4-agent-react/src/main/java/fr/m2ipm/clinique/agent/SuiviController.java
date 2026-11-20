package fr.m2ipm.clinique.agent;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Suivi des exécutions pour la page {@code /agents} (sous {@code /api/agent/**} : rôle PERSONNEL, les traces contiennent des données du dossier). */
@RestController
@RequestMapping("/api/agent/suivi")
public class SuiviController {

    private final SuiviExecution suivi;

    public SuiviController(SuiviExecution suivi) {
        this.suivi = suivi;
    }

    @GetMapping
    public List<SuiviExecution.Resume> liste() {
        return suivi.liste();
    }

    @GetMapping("/{runId}")
    public List<Evenement> evenements(@PathVariable String runId) {
        return suivi.evenements(valide(runId)).orElseThrow(() -> inconnu(runId));
    }

    @GetMapping(value = "/{runId}/flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter flux(@PathVariable String runId) {
        return suivi.abonner(valide(runId)).orElseThrow(() -> inconnu(runId));
    }

    private static String valide(String runId) {
        if (!SuiviExecution.runIdValide(runId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "runId invalide");
        }
        return runId;
    }

    private static ResponseStatusException inconnu(String runId) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Exécution inconnue : " + runId);
    }
}
