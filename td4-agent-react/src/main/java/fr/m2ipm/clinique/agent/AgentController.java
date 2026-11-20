package fr.m2ipm.clinique.agent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    public record AgentRequest(@NotBlank(message = "demande est obligatoire") String demande,
                               @Min(1) @Max(20) Integer maxEtapes) {}

    public record AgentReponse(String runId, ResultatAgent resultat) {}

    private final AgentReactService agent;
    private final TraceStore traces;
    private final SuiviExecution suivi;
    private final java.util.concurrent.ExecutorService executeur = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
    private final org.springframework.ai.chat.client.ChatClient framework;

    public AgentController(AgentReactService agent, TraceStore traces, SuiviExecution suivi,
                           @org.springframework.beans.factory.annotation.Qualifier("agentFrameworkClient")
                           org.springframework.ai.chat.client.ChatClient framework) {
        this.agent = agent;
        this.traces = traces;
        this.suivi = suivi;
        this.framework = framework;
    }

    public record FrameworkReponse(String reponse) {}

    /** Comparaison (TD4 §5) : mêmes outils, boucle du framework, aucune trace d'étape ni plafond personnalisé. */
    @PostMapping("/framework")
    public FrameworkReponse framework(@Valid @RequestBody AgentRequest requete) {
        // TODO TD4 : à écrire
        throw new UnsupportedOperationException("À écrire : framework");
    }

    @PostMapping
    public AgentReponse executer(@Valid @RequestBody AgentRequest requete) {
        // TODO TD4 : à écrire
        throw new UnsupportedOperationException("À écrire : executer");
    }

    public record Lancement(String runId) {}

    /** Lance l'agent en arrière-plan et rend aussitôt le runId : la page /agents suit l'exécution en direct. */
    @PostMapping("/lancer")
    public Lancement lancer(@Valid @RequestBody AgentRequest requete) {
        // TODO TD4 : à écrire
        throw new UnsupportedOperationException("À écrire : lancer");
    }

    @jakarta.annotation.PreDestroy
    void arreter() {
        // TODO TD4 : à écrire
        throw new UnsupportedOperationException("À écrire : arreter");
    }

    @GetMapping("/{runId}/trace")
    public ResponseEntity<List<Etape>> trace(@PathVariable String runId) {
        // TODO TD4 : à écrire
        throw new UnsupportedOperationException("À écrire : trace");
    }
}
