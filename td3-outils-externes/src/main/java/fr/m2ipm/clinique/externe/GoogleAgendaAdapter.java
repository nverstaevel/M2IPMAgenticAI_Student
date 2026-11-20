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
        try {
            identifiants.refreshIfExpired();
            return identifiants.getAccessToken().getTokenValue();
        } catch (IOException e) {
            throw new IllegalStateException("Authentification Google impossible", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LocalTime> creneauxLibres(LocalDate jour) {
        var corps = Map.of(
                "timeMin", jour.atTime(9, 0).atZone(PARIS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                "timeMax", jour.atTime(18, 0).atZone(PARIS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                "timeZone", PARIS.getId(),
                "items", List.of(Map.of("id", calendrierId)));
        Map<String, Object> reponse = client.post().uri("/freeBusy")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jeton())
                .body(corps).retrieve().body(Map.class);
        var calendriers = (Map<String, Object>) reponse.get("calendars");
        var calendrier = (Map<String, Object>) calendriers.get(calendrierId);
        var occupes = (List<Map<String, String>>) calendrier.getOrDefault("busy", List.of());
        List<LocalTime> libres = new ArrayList<>();
        for (LocalTime h = LocalTime.of(9, 0); h.isBefore(LocalTime.of(18, 0)); h = h.plusMinutes(30)) {
            ZonedDateTime debut = jour.atTime(h).atZone(PARIS);
            ZonedDateTime fin = debut.plusMinutes(30);
            boolean pris = occupes.stream().anyMatch(o ->
                    OffsetDateTime.parse(o.get("start")).toInstant().isBefore(fin.toInstant())
                            && OffsetDateTime.parse(o.get("end")).toInstant().isAfter(debut.toInstant()));
            if (!pris) libres.add(h);
        }
        return libres;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String creerEvenement(String titre, LocalDateTime debut, int dureeMinutes) {
        var fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        var corps = Map.of(
                "summary", titre,
                "start", Map.of("dateTime", debut.format(fmt), "timeZone", PARIS.getId()),
                "end", Map.of("dateTime", debut.plusMinutes(dureeMinutes).format(fmt), "timeZone", PARIS.getId()));
        Map<String, Object> reponse = client.post().uri("/calendars/{id}/events", calendrierId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jeton())
                .body(corps).retrieve().body(Map.class);
        return String.valueOf(reponse.get("id"));
    }
}
