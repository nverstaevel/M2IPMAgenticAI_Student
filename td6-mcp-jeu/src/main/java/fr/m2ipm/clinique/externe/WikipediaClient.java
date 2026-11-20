package fr.m2ipm.clinique.externe;

import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Client de l'API REST de Wikipédia (résumé d'une page, en français).
 * Wikipédia exige un en-tête User-Agent explicite ; le texte renvoyé est une source externe NON FIABLE.
 */
@Component
public class WikipediaClient {

    public static final int LONGUEUR_MAX = 1500;

    /** Résultat d'une recherche : la page existe et a un résumé, ou on explique pourquoi non. */
    public record Resume(String titre, String texte) {}

    private final RestClient client;
    private final JsonMapper json = JsonMapper.builder().build();

    @org.springframework.beans.factory.annotation.Autowired
    public WikipediaClient(@Value("${wikipedia.base-url:https://fr.wikipedia.org/api/rest_v1}") String baseUrl,
                           @Value("${wikipedia.user-agent:CliniqueFantastique/1.0 (cours M2 MIAGE; contact@example.org)}") String userAgent) {
        this(RestClient.builder().baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build());
    }

    /** Constructeur de test : client préconfiguré (serveur simulé). */
    public WikipediaClient(RestClient client) {
        this.client = client;
    }

    /**
     * Résumé de la page. Vide si la page n'existe pas.
     * @throws IllegalStateException si la page est une page d'homonymie ou si l'API est indisponible
     */
    public Optional<Resume> resume(String titre) {
        String propre = titre == null ? "" : titre.trim().replace('/', ' ').replace(' ', '_');
        if (propre.isEmpty()) {
            return Optional.empty();
        }
        String corps;
        try {
            corps = client.get().uri("/page/summary/{titre}", propre).retrieve().body(String.class);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Wikipédia est indisponible : " + e.getMessage(), e);
        }
        return analyser(corps);
    }

    /** Extrait titre et résumé de la réponse JSON ; public pour tester sur une réponse enregistrée. */
    @SuppressWarnings("unchecked")
    public Optional<Resume> analyser(String corps) {
        Map<String, Object> donnees = json.readValue(corps, Map.class);
        if ("disambiguation".equals(donnees.get("type"))) {
            throw new IllegalStateException("« " + donnees.get("title") + " » est une page d'homonymie : précisez le titre.");
        }
        Object extrait = donnees.get("extract");
        if (extrait == null || extrait.toString().isBlank()) {
            return Optional.empty();
        }
        String texte = extrait.toString();
        if (texte.length() > LONGUEUR_MAX) {
            texte = texte.substring(0, LONGUEUR_MAX) + "…";
        }
        return Optional.of(new Resume(String.valueOf(donnees.get("title")), texte));
    }
}
