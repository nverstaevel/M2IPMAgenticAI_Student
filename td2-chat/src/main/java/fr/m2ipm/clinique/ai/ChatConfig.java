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
        // TODO TD2 : à écrire
        throw new UnsupportedOperationException("À écrire : chatMemory");
    }

    /** Client sans mémoire ni outils : le triage est un appel isolé, sans identifiant de conversation. */
    @Bean
    ChatClient triageClient(ChatClient.Builder builder) {
        // TODO TD2 : à écrire
        throw new UnsupportedOperationException("À écrire : triageClient");
    }

    @Bean
    @org.springframework.context.annotation.Primary
    ChatClient drGribouille(ChatClient.Builder builder, ChatMemory memoire, OutilsClinique outils) {
        // TODO TD2 : à écrire
        throw new UnsupportedOperationException("À écrire : drGribouille");
    }
}
