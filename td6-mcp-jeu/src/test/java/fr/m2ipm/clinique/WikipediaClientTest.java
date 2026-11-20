package fr.m2ipm.clinique;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import fr.m2ipm.clinique.externe.WikipediaClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Test de l'outil Wikipédia sans réseau : réponses JSON de référence + serveur simulé. */
class WikipediaClientTest {

    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://wiki.test/api")
            .defaultHeader(HttpHeaders.USER_AGENT, "TestClinique/1.0");
    private final MockRestServiceServer serveur = MockRestServiceServer.bindTo(builder).build();
    private final WikipediaClient client = new WikipediaClient(builder.build());

    private static String fixture(String nom) throws Exception {
        return Files.readString(Path.of("src/test/resources/wikipedia/" + nom), StandardCharsets.UTF_8);
    }

    @Test
    void pageExistante() throws Exception {
        serveur.expect(requestTo("http://wiki.test/api/page/summary/Dragon"))
                .andExpect(header(HttpHeaders.USER_AGENT, "TestClinique/1.0"))
                .andRespond(withSuccess(fixture("wikipedia-dragon.json"), MediaType.APPLICATION_JSON));
        var resume = client.resume("Dragon");
        assertThat(resume).isPresent();
        assertThat(resume.get().titre()).isEqualTo("Dragon");
        assertThat(resume.get().texte()).contains("créature légendaire");
        serveur.verify();
    }

    @Test
    void pageInexistante() {
        serveur.expect(requestTo("http://wiki.test/api/page/summary/Zorglub"))
                .andRespond(withResourceNotFound());
        assertThat(client.resume("Zorglub")).isEmpty();
    }

    @Test
    void pageDHomonymie() throws Exception {
        serveur.expect(requestTo("http://wiki.test/api/page/summary/Ph%C3%A9nix"))
                .andRespond(withSuccess(fixture("wikipedia-homonymie.json"), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.resume("Phénix")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("homonymie");
    }

    @Test
    void resumeTronqueA1500Caracteres() throws Exception {
        var resume = client.analyser(fixture("wikipedia-long.json")).orElseThrow();
        assertThat(resume.texte().length()).isEqualTo(WikipediaClient.LONGUEUR_MAX + 1);
        assertThat(resume.texte()).endsWith("…");
    }

    @Test
    void titreVideNInterrogePasLeReseau() {
        assertThat(client.resume("  ")).isEmpty();
        serveur.verify();
    }
}
