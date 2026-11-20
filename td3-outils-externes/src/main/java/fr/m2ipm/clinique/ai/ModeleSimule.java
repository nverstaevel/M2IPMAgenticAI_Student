package fr.m2ipm.clinique.ai;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Faux modèle déterministe (profil « simule ») : tests et évaluation sans serveur de modèles.
 * <ul>
 *   <li>un message « outil:NOM:{json} » provoque l'appel de l'outil NOM avec ces arguments ;</li>
 *   <li>après le résultat de l'outil, il répond « Résultat des outils : … » ;</li>
 *   <li>une demande de sortie structurée (le prompt contient « JSON ») reçoit un triage fixe ;</li>
 *   <li>sinon il répond « Simulé : » suivi du message.</li>
 * </ul>
 */
@Component
@Profile("simule")
public class ModeleSimule implements ChatModel {

    static final String TRIAGE_JSON = "{\"espece\":\"Dragon\",\"urgence\":\"MOYENNE\","
            + "\"resume\":\"Rhume de dragon simulé\",\"symptomes\":[\"fumée verdâtre\",\"éternuements\"]}";

    @Override
    public ChatResponse call(Prompt prompt) {
        List<Message> messages = prompt.getInstructions();
        Message dernier = messages.get(messages.size() - 1);
        if (dernier instanceof ToolResponseMessage retour) {
            String donnees = retour.getResponses().stream()
                    .map(ToolResponseMessage.ToolResponse::responseData)
                    .collect(Collectors.joining(" | "));
            return reponse("Résultat des outils : " + donnees);
        }
        String texte = dernier.getText() == null ? "" : dernier.getText();
        if (texte.startsWith("outil:")) {
            String[] morceaux = texte.split(":", 3);
            AssistantMessage appel = AssistantMessage.builder()
                    .content("")
                    .toolCalls(List.of(new AssistantMessage.ToolCall("appel-1", "function", morceaux[1], morceaux[2])))
                    .build();
            return new ChatResponse(List.of(new Generation(appel)));
        }
        if (texte.contains("JSON")) {
            return reponse(TRIAGE_JSON);
        }
        return reponse("Simulé : " + texte);
    }

    /**
     * Spring AI 2.0 : le ChatClient part de {@code chatModel.getOptions()}. Sans options « tool calling »,
     * le ToolCallingAdvisor ignore la requête et la boucle d'outils ne démarre pas.
     */
    @Override
    public ChatOptions getOptions() {
        return ToolCallingChatOptions.builder().build();
    }

    private static ChatResponse reponse(String texte) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(texte))));
    }
}
