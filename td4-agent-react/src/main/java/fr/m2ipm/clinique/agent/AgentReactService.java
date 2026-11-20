package fr.m2ipm.clinique.agent;

import fr.m2ipm.clinique.ai.OutilsClinique;
import fr.m2ipm.clinique.externe.OutilsExternes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Boucle ReAct écrite à la main : le ChatModel est appelé directement et ne lance jamais les outils
 * (en Spring AI 2.0 le modèle ne lance plus les outils, c'est le ToolCallingAdvisor) ; c'est cette classe qui exécute les outils, compte, trace et décide de s'arrêter.
 */
@Service
public class AgentReactService {

    public static final String PROMPT_SYSTEME = """
            Tu es l'agent de la clinique des créatures fantastiques. Tu accomplis la demande en enchaînant des outils.
            Avant CHAQUE appel d'outil, écris une phrase qui dit ce que tu cherches et pourquoi.
            N'invente jamais un identifiant, un tarif, un stock ou un créneau : obtiens-les avec un outil.
            Si un outil ne trouve rien, ou si la demande est impossible ou absurde, dis-le et demande une précision.
            Tu ne peux que PROPOSER des consultations ou des événements : un membre du personnel confirme.
            Quand tu as tout ce qu'il faut, réponds à l'utilisateur sans appeler d'outil.
            """;

    static final int LONGUEUR_RESULTAT = 500;

    private final ChatModel modele;
    private final ToolCallback[] outils;
    private final TraceStore traces;
    private final SuiviExecution suivi;
    private final ToolCallingManager gestionnaire = ToolCallingManager.builder().build();
    private final int budgetTokens;

    @Autowired
    public AgentReactService(ChatModel modele, OutilsClinique clinique, OutilsExternes externes, TraceStore traces,
                             SuiviExecution suivi, @Value("${agent.budget-tokens:20000}") int budgetTokens) {
        this(modele, ToolCallbacks.from(clinique, externes), traces, suivi, budgetTokens);
    }

    /** Constructeur de test : outils quelconques, modèle scripté, suivi en mémoire. */
    public AgentReactService(ChatModel modele, ToolCallback[] outils, TraceStore traces, int budgetTokens) {
        this(modele, outils, traces, new SuiviExecution(), budgetTokens);
    }

    public AgentReactService(ChatModel modele, ToolCallback[] outils, TraceStore traces, SuiviExecution suivi, int budgetTokens) {
        this.modele = modele;
        this.outils = outils;
        this.traces = traces;
        this.suivi = suivi;
        this.budgetTokens = budgetTokens;
    }

    public record Execution(String runId, ResultatAgent resultat) {}

    public Execution executer(String demande, int maxEtapes) {
        // TODO TD4 : à écrire
        throw new UnsupportedOperationException("À écrire : executer");
    }

    public ResultatAgent executer(String runId, String demande, int maxEtapes) {
        // TODO TD4 : à écrire
        throw new UnsupportedOperationException("À écrire : executer");
    }

    /** Options de l'appel : on part de celles du modèle (OpenAiChatOptions, AnthropicChatOptions…) pour ne pas casser le client du fournisseur. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private ToolCallingChatOptions optionsAppel(Map<String, Object> contexte, Double temperature) {
        // TODO TD4 : à écrire
        throw new UnsupportedOperationException("À écrire : optionsAppel");
    }

    /** Nœud final du graphe (réponse ou motif d'arrêt), puis fin du suivi. */
    private ResultatAgent cloturer(String runId, String parent, ResultatAgent r, String message) {
        // TODO TD4 : à écrire
        throw new UnsupportedOperationException("À écrire : cloturer");
    }

    private static String abreger(String s) {
        // TODO TD4 : à écrire
        throw new UnsupportedOperationException("À écrire : abreger");
    }

    private void enregistrer(String runId, List<Etape> etapes, Etape etape) {
        // TODO TD4 : à écrire
        throw new UnsupportedOperationException("À écrire : enregistrer");
    }

    private static String resultatsOutils(List<Message> historique) {
        // TODO TD4 : à écrire
        throw new UnsupportedOperationException("À écrire : resultatsOutils");
    }

    private static String tronquer(String s) {
        // TODO TD4 : à écrire
        throw new UnsupportedOperationException("À écrire : tronquer");
    }

    private static long ms(long debutNanos) {
        // TODO TD4 : à écrire
        throw new UnsupportedOperationException("À écrire : ms");
    }
}
