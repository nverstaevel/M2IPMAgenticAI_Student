package fr.m2ipm.clinique;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/** Modèle scripté : rejoue des réponses dans l'ordre, puis répète la dernière. Garde les prompts reçus. */
class ModeleScenario implements ChatModel {

    final List<ChatResponse> reponses = new ArrayList<>();
    final List<Prompt> recus = new ArrayList<>();

    ModeleScenario appel(String raisonnement, String outil, String json) {
        var tc = new AssistantMessage.ToolCall("id-" + (reponses.size() + 1), "function", outil, json);
        reponses.add(new ChatResponse(List.of(new Generation(
                AssistantMessage.builder().content(raisonnement).toolCalls(List.of(tc)).build()))));
        return this;
    }

    ModeleScenario finale(String texte) {
        reponses.add(new ChatResponse(List.of(new Generation(new AssistantMessage(texte)))));
        return this;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        recus.add(prompt);
        return reponses.get(Math.min(recus.size(), reponses.size()) - 1);
    }
}
