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
 *
 * 🧠 À COMPLÉTER dans ce fichier : 6 zones marquées « TODO » (TD4). Tout le reste est fourni et testé : lisez-le, ne le modifiez pas.
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
        String runId = UUID.randomUUID().toString();
        return new Execution(runId, executer(runId, demande, maxEtapes));
    }

    public ResultatAgent executer(String runId, String demande, int maxEtapes) {
        suivi.demarrer(runId, "ReAct : " + abreger(demande));
        suivi.noeud(runId, "demande", List.of(), Evenement.UTILISATEUR, "Demande", Evenement.OK, demande, null, null, null);
        ToolCallingChatOptions options = optionsAppel(Map.of("conversationId", runId), null);
        Prompt prompt = new Prompt(List.<Message>of(new SystemMessage(PROMPT_SYSTEME), new UserMessage(demande)), options);
        List<Etape> etapes = new ArrayList<>();
        int tokens = 0;
        String appelPrecedent = null;
        String precedent = "demande";
        String enCours = null;
        ResultatAgent resultatFinal;
        try {
            for (int numero = 1; ; numero++) {
                String idModele = "e" + numero;
                enCours = idModele;
                suivi.noeud(runId, idModele, List.of(precedent), Evenement.MODELE, "Étape " + numero + " · modèle",
                        Evenement.EN_COURS, null, null, null, null);
                long debut = System.nanoTime();
                final Prompt promptTour = prompt;
                // TODO TD4 (étape 1) : appelez le modèle avec `promptTour`.
                // Astuce : enveloppez l'appel dans SuiviOutils.reessayer(suivi, runId, idModele, () -> …) ;
                // le graphe affichera alors les nouvelles tentatives.
                ChatResponse reponse = null;
                AssistantMessage sortie = reponse.getResult().getOutput();
                String texte = sortie.getText() == null ? "" : sortie.getText();
                Jetons jetons = Jetons.de(reponse, prompt, texte);
                int tokensEtape = jetons.total();
                tokens += tokensEtape;
                suivi.maj(runId, idModele, Evenement.OK, texte, null, ms(debut), tokensEtape);
                suivi.jetons(runId, idModele, jetons.entree(), jetons.sortie(), 1, jetons.estime());

                // TODO TD4 (étape 1) : si le modèle ne demande aucun outil, c'est la réponse finale.
                // Enregistrez l'étape (enregistrer), construisez le ResultatAgent (MotifArret.REPONSE_FINALE)
                // et terminez avec cloturer(runId, idModele, resultatFinal, texte).

                var appels = sortie.getToolCalls();
                String noms = appels.stream().map(AssistantMessage.ToolCall::name).collect(Collectors.joining("+"));
                String arguments = appels.size() == 1 ? appels.get(0).arguments()
                        : appels.stream().map(AssistantMessage.ToolCall::arguments).collect(Collectors.joining(",", "[", "]"));
                String signature = noms + arguments;
                String idOutil = idModele + "-outil";
                enCours = idOutil;
                suivi.noeud(runId, idOutil, List.of(idModele), Evenement.OUTIL, noms, Evenement.EN_COURS, arguments, null, null, null);

                // TODO TD4 (étape 2) : arrêt BOUCLE_DETECTEE. Si `signature` est égale à `appelPrecedent`
                // (Objects.equals), n'exécutez PAS l'outil : enregistrez l'étape avec un résultat explicite,
                // marquez le nœud outil en erreur (suivi.maj(..., Evenement.ERREUR, ...)) et terminez avec cloturer.
                appelPrecedent = signature;

                long debutOutil = System.nanoTime();
                // TODO TD4 (étape 1) : faites exécuter les outils demandés par le ToolCallingManager (`gestionnaire`).
                ToolExecutionResult execution = null;
                String resultat = resultatsOutils(execution.conversationHistory());
                suivi.maj(runId, idOutil, Evenement.OK, null, tronquer(resultat), ms(debutOutil), null);
                enregistrer(runId, etapes, new Etape(numero, texte, noms, arguments, tronquer(resultat), ms(debut), tokensEtape));
                precedent = idOutil;

                if (tokens >= budgetTokens) {
                    resultatFinal = new ResultatAgent("Arrêt : budget de " + budgetTokens + " tokens atteint.",
                            MotifArret.BUDGET_TOKENS, etapes, tokens);
                    return cloturer(runId, idOutil, resultatFinal, resultatFinal.reponse());
                }
                // TODO TD4 (étape 2) : arrêt PLAFOND_ETAPES quand numero >= maxEtapes (même forme que le budget ci-dessus).
                // TODO TD4 (étape 1) : le prompt du tour suivant reprend l'historique renvoyé par l'exécution des outils
                // (execution.conversationHistory()) et les mêmes `options`.
            }
        } catch (RuntimeException e) {
            if (enCours != null) {
                suivi.maj(runId, enCours, Evenement.ERREUR, null, String.valueOf(e.getMessage()), null, null);
            }
            resultatFinal = new ResultatAgent("Erreur : " + e.getMessage(), MotifArret.ERREUR, etapes, tokens);
            return cloturer(runId, enCours == null ? "demande" : enCours, resultatFinal, resultatFinal.reponse());
        }
    }

    /** Options de l'appel : on part de celles du modèle (OpenAiChatOptions, AnthropicChatOptions…) pour ne pas casser le client du fournisseur. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private ToolCallingChatOptions optionsAppel(Map<String, Object> contexte, Double temperature) {
        try {
            var defaut = modele.getOptions();
            if (defaut != null) {
                var b = (org.springframework.ai.chat.prompt.ChatOptions.Builder) defaut.mutate();
                if (b instanceof ToolCallingChatOptions.Builder tb) {
                    tb.toolCallbacks(outils).toolContext(contexte);
                    if (temperature != null) { tb.temperature(temperature); }
                    return (ToolCallingChatOptions) tb.build();
                }
            }
        } catch (RuntimeException | LinkageError e) {
            // repli ci-dessous
        }
        var constructeur = ToolCallingChatOptions.builder().toolCallbacks(outils).toolContext(contexte);
        if (temperature != null) { constructeur.temperature(temperature); }
        return constructeur.build();
    }

    /** Nœud final du graphe (réponse ou motif d'arrêt), puis fin du suivi. */
    private ResultatAgent cloturer(String runId, String parent, ResultatAgent r, String message) {
        String libelle = r.motif() == MotifArret.REPONSE_FINALE ? "Réponse finale" : "Arrêt : " + r.motif();
        suivi.noeud(runId, "fin", List.of(parent), Evenement.FIN, libelle,
                r.motif() == MotifArret.REPONSE_FINALE ? Evenement.OK : Evenement.ERREUR, message, null, null, r.tokens());
        suivi.terminer(runId, r.motif().name());
        return r;
    }

    private static String abreger(String s) {
        return s.length() > 60 ? s.substring(0, 60) + "…" : s;
    }

    private void enregistrer(String runId, List<Etape> etapes, Etape etape) {
        etapes.add(etape);
        traces.ajouter(runId, etape);
    }

    private static String resultatsOutils(List<Message> historique) {
        Message dernier = historique.get(historique.size() - 1);
        if (dernier instanceof ToolResponseMessage retour) {
            return retour.getResponses().stream().map(ToolResponseMessage.ToolResponse::responseData)
                    .collect(Collectors.joining(" | "));
        }
        return "";
    }

    private static String tronquer(String s) {
        return s.length() > LONGUEUR_RESULTAT ? s.substring(0, LONGUEUR_RESULTAT) + "…" : s;
    }

    private static long ms(long debutNanos) {
        return (System.nanoTime() - debutNanos) / 1_000_000;
    }
}
