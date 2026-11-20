package fr.m2ipm.clinique.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    /** Fenêtre glissante de 10 messages, stockée par le dépôt JDBC (autoconfiguré). */
    @Bean
    ChatMemory chatMemory(ChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder().chatMemoryRepository(repository).maxMessages(10).build();
    }

    /** Client sans mémoire ni outils : le triage est un appel isolé, sans identifiant de conversation. */
    @Bean
    ChatClient triageClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("Tu es l'infirmier de tri de la clinique des créatures fantastiques. "
                        + "Résume la demande et estime l'urgence.")
                .build();
    }

    @Bean
    @org.springframework.context.annotation.Primary
    ChatClient drGribouille(ChatClient.Builder builder, ChatMemory memoire, OutilsClinique outils,
                         fr.m2ipm.clinique.externe.OutilsExternes externes) {
        return builder
                .defaultSystem(PromptsGribouille.BASE)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(memoire).build(),
                        new SimpleLoggerAdvisor())
                .defaultTools(outils, externes)
                .build();
    }
}
