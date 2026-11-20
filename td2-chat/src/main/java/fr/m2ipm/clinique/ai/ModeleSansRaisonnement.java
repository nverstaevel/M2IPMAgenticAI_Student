package fr.m2ipm.clinique.ai;

import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * Enveloppe d'un {@link ChatModel} qui retire le raisonnement du modèle (métadonnée « reasoningContent ») des messages
 * de l'assistant avant de les renvoyer au fournisseur. Spring AI 2.0.0 rejoue ce raisonnement dans le champ
 * {@code reasoning_content}, que Groq refuse (400) avec les modèles de raisonnement (gpt-oss, qwen3…).
 * Actif uniquement avec le profil « groq » (voir {@link SansRaisonnementConfig}).
 */
public class ModeleSansRaisonnement implements ChatModel {

    private static final String CLE_RAISONNEMENT = "reasoningContent";

    private final ChatModel delegue;

    public ModeleSansRaisonnement(ChatModel delegue) {
        this.delegue = delegue;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        return delegue.call(nettoyer(prompt));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return delegue.stream(nettoyer(prompt));
    }

    /** Le ChatClient part de {@code chatModel.getOptions()} (Spring AI 2.0) : à déléguer, sinon plus d'outils. */
    @Override
    public ChatOptions getOptions() {
        return delegue.getOptions();
    }

    static Prompt nettoyer(Prompt prompt) {
        List<Message> messages = prompt.getInstructions();
        for (Message m : messages) {
            if (m instanceof AssistantMessage a && a.getMetadata().containsKey(CLE_RAISONNEMENT)) {
                try {
                    a.getMetadata().remove(CLE_RAISONNEMENT);
                } catch (UnsupportedOperationException e) {
                    throw new IllegalStateException("Métadonnées de l'assistant non modifiables : impossible de retirer le raisonnement", e);
                }
            }
        }
        return prompt;
    }
}
