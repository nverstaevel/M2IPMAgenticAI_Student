package fr.m2ipm.clinique.controller;

import fr.m2ipm.clinique.agent.SuiviExecution;
import fr.m2ipm.clinique.roles.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.security.Principal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class RolesController {

    public record DuoRequest(@Min(1) int casId, @NotNull ProfilActeur profil) {}
    public record ValidationRequest(boolean valide) {}
    public record SuperviseurRequest(@NotBlank(message = "demande est obligatoire") String demande) {}
    public record WorkflowRequest(@NotBlank String creature, @NotBlank String symptome, LocalDate jour) {}

    /** Vue JSON d'un dossier (le PDF en est la version imprimable). */
    public record DossierVue(String runId, CasBestiaire cas, ProfilActeur profil, StatutDuo statut, Validation validation,
                             String validePar, List<Tour> tours, long dureeMs) {
        static DossierVue de(DossierRun r) {
            return new DossierVue(r.runId(), r.cas(), r.profil(), r.statut(), r.validation(), r.validePar(), r.tours(), r.dureeMs());
        }
    }

    private final DuoService duo;
    private final CasBestiaireRepository cas;
    private final DossierPdfService pdf;
    private final SuperviseurAgent superviseur;
    private final TraiterConsultationWorkflow workflow;
    private final SuiviExecution suivi;
    private final ExecutorService executeur = Executors.newVirtualThreadPerTaskExecutor();
    private final Clock horloge = Clock.systemDefaultZone();

    public RolesController(DuoService duo, CasBestiaireRepository cas, DossierPdfService pdf,
                           SuperviseurAgent superviseur, TraiterConsultationWorkflow workflow, SuiviExecution suivi) {
        this.duo = duo;
        this.cas = cas;
        this.pdf = pdf;
        this.superviseur = superviseur;
        this.workflow = workflow;
        this.suivi = suivi;
    }

    @GetMapping("/cas")
    public List<CasBestiaire> cas() { return cas.tous(); }

    @PostMapping("/duo")
    public DossierVue duo(@Valid @RequestBody DuoRequest r) {
        CasBestiaire c = cas.parId(r.casId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cas inconnu : " + r.casId()));
        return DossierVue.de(duo.traiter(c, r.profil()));
    }

    @GetMapping("/dossiers/{runId}")
    public DossierVue dossier(@PathVariable String runId) { return DossierVue.de(trouver(runId)); }

    @GetMapping(value = "/dossiers/{runId}.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public byte[] pdf(@PathVariable String runId) {
        trouver(runId);
        return pdf.generer(runId);
    }

    /** Validation humaine, réservée au rôle PERSONNEL : c'est elle (et rien d'autre) qui autorise l'application du traitement. */
    @PostMapping("/dossiers/{runId}/valider")
    public DossierVue valider(@PathVariable String runId, @RequestBody ValidationRequest r, Principal agent) {
        DossierRun run = trouver(runId);
        try {
            run.decider(r.valide(), agent.getName(), horloge.instant());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        pdf.generer(runId);
        return DossierVue.de(run);
    }

    @PostMapping("/superviseur")
    public SuperviseurAgent.Resultat superviseur(@Valid @RequestBody SuperviseurRequest r) { return superviseur.executer(r.demande()); }

    @PostMapping("/workflow")
    public ResponseEntity<?> workflow(@Valid @RequestBody WorkflowRequest r) {
        try {
            return ResponseEntity.ok(workflow.traiter(r.creature(), r.symptome(), r.jour() == null ? LocalDate.now().plusDays(1) : r.jour()));
        } catch (TraiterConsultationWorkflow.EchecWorkflow e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        }
    }

    public record Lancement(String runId) {}

    /** Lancements asynchrones pour la page /agents (PERSONNEL) : rendent le runId aussitôt, le graphe se construit en direct. */
    @PostMapping("/agent/lancer-duo")
    public Lancement lancerDuo(@Valid @RequestBody DuoRequest r) {
        CasBestiaire c = cas.parId(r.casId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cas inconnu : " + r.casId()));
        String runId = UUID.randomUUID().toString();
        suivi.demarrer(runId, "Duo : cas " + c.id() + " · " + c.creature());
        executeur.submit(() -> {
            try { duo.traiter(runId, c, r.profil()); } catch (RuntimeException e) { /* déjà visible dans le graphe */ }
        });
        return new Lancement(runId);
    }

    @PostMapping("/agent/lancer-superviseur")
    public Lancement lancerSuperviseur(@Valid @RequestBody SuperviseurRequest r) {
        String runId = UUID.randomUUID().toString();
        suivi.demarrer(runId, "Superviseur : " + r.demande());
        executeur.submit(() -> {
            try { superviseur.executer(runId, r.demande()); } catch (RuntimeException e) { /* déjà visible dans le graphe */ }
        });
        return new Lancement(runId);
    }

    @PostMapping("/agent/lancer-workflow")
    public Lancement lancerWorkflow(@Valid @RequestBody WorkflowRequest r) {
        String runId = UUID.randomUUID().toString();
        suivi.demarrer(runId, "Workflow : " + r.creature());
        LocalDate jour = r.jour() == null ? LocalDate.now().plusDays(1) : r.jour();
        executeur.submit(() -> {
            try { workflow.traiter(runId, r.creature(), r.symptome(), jour); } catch (RuntimeException e) { /* déjà visible dans le graphe */ }
        });
        return new Lancement(runId);
    }

    @jakarta.annotation.PreDestroy
    void arreter() { executeur.shutdownNow(); }

    private DossierRun trouver(String runId) {
        return duo.run(runId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier inconnu : " + runId));
    }
}
