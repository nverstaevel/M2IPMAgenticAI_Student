package fr.m2ipm.clinique;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/** Modèle de test : rejoue des réponses texte (JSON) dans l'ordre, puis répète la dernière ; garde les prompts reçus. */
class ModeleTexte implements ChatModel {

    final List<String> reponses;
    final List<Prompt> recus = new ArrayList<>();

    ModeleTexte(String... reponses) { this.reponses = List.of(reponses); }

    @Override
    public ChatResponse call(Prompt prompt) {
        recus.add(prompt);
        String r = reponses.get(Math.min(recus.size(), reponses.size()) - 1);
        return new ChatResponse(List.of(new Generation(new AssistantMessage(r))));
    }

    String dernierUtilisateur() {
        var m = recus.get(recus.size() - 1).getInstructions();
        return m.get(m.size() - 1).getText();
    }
}
