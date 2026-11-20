package fr.m2ipm.clinique.externe;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** agenda.mode=simule (défaut) ou google. L'adaptateur Google demande agenda.google.calendar-id et agenda.google.credentials-file. */
@Configuration
public class AgendaConfig {

    @Bean
    @ConditionalOnProperty(name = "agenda.mode", havingValue = "simule", matchIfMissing = true)
    AgendaPort agendaSimule() {
        return new AgendaSimule();
    }

    @Bean
    @ConditionalOnProperty(name = "agenda.mode", havingValue = "google")
    AgendaPort agendaGoogle(@Value("${agenda.google.calendar-id}") String calendrier,
                            @Value("${agenda.google.credentials-file}") String fichier) {
        return new GoogleAgendaAdapter(calendrier, fichier);
    }
}
