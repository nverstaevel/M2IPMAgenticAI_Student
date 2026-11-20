package fr.m2ipm.clinique.ai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Profil « groq » : enveloppe le ChatModel pour ne pas rejouer le raisonnement (voir {@link ModeleSansRaisonnement}). */
@Configuration
@Profile("groq")
public class SansRaisonnementConfig {

    @Bean
    static BeanPostProcessor enveloppeSansRaisonnement() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String nom) {
                return bean instanceof ChatModel m && !(bean instanceof ModeleSansRaisonnement)
                        ? new ModeleSansRaisonnement(m) : bean;
            }
        };
    }
}
