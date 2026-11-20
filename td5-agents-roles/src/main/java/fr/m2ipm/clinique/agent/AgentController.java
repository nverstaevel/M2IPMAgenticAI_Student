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
        return new FrameworkReponse(framework.prompt().user(requete.demande())
                .toolContext(java.util.Map.of("conversationId", "framework")).call().content());
    }

    @PostMapping
    public AgentReponse executer(@Valid @RequestBody AgentRequest requete) {
        var e = agent.executer(requete.demande(), requete.maxEtapes() == null ? 8 : requete.maxEtapes());
        return new AgentReponse(e.runId(), e.resultat());
    }

    public record Lancement(String runId) {}

    /** Lance l'agent en arrière-plan et rend aussitôt le runId : la page /agents suit l'exécution en direct. */
    @PostMapping("/lancer")
    public Lancement lancer(@Valid @RequestBody AgentRequest requete) {
        String runId = java.util.UUID.randomUUID().toString();
        int max = requete.maxEtapes() == null ? 8 : requete.maxEtapes();
        suivi.demarrer(runId, "ReAct : " + (requete.demande().length() > 60 ? requete.demande().substring(0, 60) + "…" : requete.demande()));
        executeur.submit(() -> agent.executer(runId, requete.demande(), max));
        return new Lancement(runId);
    }

    @jakarta.annotation.PreDestroy
    void arreter() {
        executeur.shutdownNow();
    }

    @GetMapping("/{runId}/trace")
    public ResponseEntity<List<Etape>> trace(@PathVariable String runId) {
        List<Etape> etapes = traces.lire(runId);
        return etapes.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(etapes);
    }
}
