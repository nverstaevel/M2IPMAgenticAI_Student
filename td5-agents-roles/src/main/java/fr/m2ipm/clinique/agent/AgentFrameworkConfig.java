package fr.m2ipm.clinique.agent;

import fr.m2ipm.clinique.ai.OutilsClinique;
import fr.m2ipm.clinique.externe.OutilsExternes;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Même agent, boucle du framework : le ChatClient (ToolCallingAdvisor) enchaîne les outils sans qu'on voie les étapes. */
@Configuration
public class AgentFrameworkConfig {

    @Bean
    ChatClient agentFrameworkClient(ChatClient.Builder builder, OutilsClinique clinique, OutilsExternes externes) {
        return builder.defaultSystem(AgentReactService.PROMPT_SYSTEME).defaultTools(clinique, externes).build();
    }
}
