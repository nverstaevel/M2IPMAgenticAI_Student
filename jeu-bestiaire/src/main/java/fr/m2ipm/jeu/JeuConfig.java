package fr.m2ipm.jeu;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Publie les outils de {@link OutilsJeu} : le starter MCP serveur expose tout bean {@link ToolCallbackProvider}. */
@Configuration
public class JeuConfig {

    @Bean
    ToolCallbackProvider outilsDuJeu(OutilsJeu outils) {
        return MethodToolCallbackProvider.builder().toolObjects(outils).build();
    }
}
