package fr.m2ipm.clinique.externe;

import com.google.auth.oauth2.GoogleCredentials;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/**
 * Adaptateur Google Agenda (API REST v3) avec un compte de service. À utiliser avec un agenda DE TEST partagé
 * avec l'adresse du compte de service. Aucun invité n'est jamais ajouté aux événements.
 * Non vérifié automatiquement : exige un vrai compte de service (voir README).
 */
public class GoogleAgendaAdapter implements AgendaPort {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final String PORTEE = "https://www.googleapis.com/auth/calendar";

    private final String calendrierId;
    private final GoogleCredentials identifiants;
    private final RestClient client = RestClient.builder().baseUrl("https://www.googleapis.com/calendar/v3").build();

    public GoogleAgendaAdapter(String calendrierId, String fichierIdentifiants) {
        this.calendrierId = calendrierId;
        try (var in = new FileInputStream(fichierIdentifiants)) {
            this.identifiants = GoogleCredentials.fromStream(in).createScoped(PORTEE);
        } catch (IOException e) {
            throw new IllegalStateException("Fichier de compte de service illisible : " + fichierIdentifiants, e);
        }
    }

    private String jeton() {
        // TODO TD3 : à écrire
        throw new UnsupportedOperationException("À écrire : jeton");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LocalTime> creneauxLibres(LocalDate jour) {
        // TODO TD3 : à écrire
        throw new UnsupportedOperationException("À écrire : creneauxLibres");
    }

    @Override
    @SuppressWarnings("unchecked")
    public String creerEvenement(String titre, LocalDateTime debut, int dureeMinutes) {
        // TODO TD3 : à écrire
        throw new UnsupportedOperationException("À écrire : creerEvenement");
    }
}
