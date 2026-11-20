package fr.m2ipm.clinique;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Duo, dossier PDF, validation, superviseur et workflow avec le modèle SIMULÉ (gardien à règles) :
 * ces tests valident la mécanique, pas la qualité d'un vrai LLM.
 */
@SpringBootTest(properties = {"dossiers.dir=target/dossiers-test", "agent.traces.dir=target/traces-test"})
@ActiveProfiles("simule")
class RolesApiTest {

    @Autowired WebApplicationContext contexte;
    MockMvc mvc;

    @BeforeEach
    void init() {
        mvc = MockMvcBuilders.webAppContextSetup(contexte).apply(springSecurity()).build();
    }

    private String duo(int casId, String profil) throws Exception {
        return mvc.perform(post("/api/duo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"casId\":" + casId + ",\"profil\":\"" + profil + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    @WithMockUser(roles = "PERSONNEL")
    void lesCinqCasDuBestiaire() throws Exception {
        String[] premierVerdict = {"REJETE", "REJETE", "APPROUVE", "APPROUVE", "DOUTE"};
        String[] statut = {"APPROUVEE", "APPROUVEE", "APPROUVEE", "APPROUVEE", "VERS_HUMAIN"};
        int[] tours = {2, 2, 1, 1, 1};
        for (int i = 1; i <= 5; i++) {
            String corps = duo(i, "PRESSE");
            assertThat((String) JsonPath.read(corps, "$.tours[0].critique.verdict")).isEqualTo(premierVerdict[i - 1]);
            assertThat((String) JsonPath.read(corps, "$.statut")).isEqualTo(statut[i - 1]);
            assertThat((int) JsonPath.read(corps, "$.tours.length()")).isEqualTo(tours[i - 1]);
            assertThat((String) JsonPath.read(corps, "$.validation")).isEqualTo("EN_ATTENTE_DE_VALIDATION");
            String runId = JsonPath.read(corps, "$.runId");

            byte[] pdf = mvc.perform(get("/api/dossiers/" + runId + ".pdf"))
                    .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andReturn().getResponse().getContentAsByteArray();
            assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
            assertThat(Files.exists(Path.of("target/dossiers-test", runId + ".pdf"))).isTrue();
            try (var doc = Loader.loadPDF(pdf)) {
                String texte = new PDFTextStripper().getText(doc);
                assertThat(texte).contains("Dossier de traitement").contains("Tour 1").contains("EN_ATTENTE_DE_VALIDATION")
                        .contains("wikipedia.org/wiki/").contains("fictif");
            }
        }
    }

    @Test
    @WithMockUser(roles = "PERSONNEL")
    void lePrudentEvitelErreurDuPresse() throws Exception {
        String corps = duo(1, "PRUDENT");
        assertThat((String) JsonPath.read(corps, "$.tours[0].critique.verdict")).isEqualTo("APPROUVE");
    }

    @Test
    @WithMockUser(username = "gribouille", roles = "PERSONNEL")
    void validationHumaineUneSeuleFois() throws Exception {
        String runId = JsonPath.read(duo(3, "PRESSE"), "$.runId");
        mvc.perform(post("/api/dossiers/" + runId + "/valider").contentType(MediaType.APPLICATION_JSON).content("{\"valide\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validation").value("VALIDE"))
                .andExpect(jsonPath("$.validePar").value("gribouille"));
        mvc.perform(post("/api/dossiers/" + runId + "/valider").contentType(MediaType.APPLICATION_JSON).content("{\"valide\":false}"))
                .andExpect(status().isConflict());
        byte[] pdf = mvc.perform(get("/api/dossiers/" + runId + ".pdf")).andReturn().getResponse().getContentAsByteArray();
        try (var doc = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(doc)).contains("VALIDE par gribouille");
        }
    }

    @Test
    @WithMockUser(roles = "LECTURE")
    void lesDossiersSontReservesAuPersonnel() throws Exception {
        mvc.perform(post("/api/duo").contentType(MediaType.APPLICATION_JSON).content("{\"casId\":1,\"profil\":\"PRESSE\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/dossiers/x.pdf")).andExpect(status().isForbidden());
        mvc.perform(get("/dossiers")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PERSONNEL")
    void casInconnuEtPageDesDossiers() throws Exception {
        mvc.perform(post("/api/duo").contentType(MediaType.APPLICATION_JSON).content("{\"casId\":99,\"profil\":\"PRESSE\"}"))
                .andExpect(status().isNotFound());
        duo(4, "PRESSE");
        mvc.perform(get("/dossiers")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Dossiers de traitement")))
                .andExpect(content().string(containsString("Rebelote")));
    }

    @Test
    @WithMockUser(roles = "PERSONNEL")
    void superviseurDelegueAuxTroisTravailleurs() throws Exception {
        mvc.perform(post("/api/superviseur").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"demande\":\"Flammèche a de la fièvre : traitement, devis, rendez-vous\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapes.length()").value(3))
                .andExpect(jsonPath("$.etapes[0].outil").value("DOSSIERWorker"))
                .andExpect(jsonPath("$.etapes[1].outil").value("PHARMACIEWorker"))
                .andExpect(jsonPath("$.etapes[2].outil").value("AGENDAWorker"));
    }

    @Test
    @WithMockUser(roles = "PERSONNEL")
    void workflowClassiqueSansModele() throws Exception {
        mvc.perform(post("/api/workflow").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"creature\":\"Paillette\",\"symptome\":\"sabot fêlé\",\"jour\":\"2026-12-03\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicament").value("Onguent de givre"))
                .andExpect(jsonPath("$.tarifConsultation").value(60.0))
                .andExpect(jsonPath("$.soigneur.nom").value("Dr Gribouille"))
                .andExpect(jsonPath("$.creneau").value("2026-12-03T09:00:00"));
        mvc.perform(post("/api/workflow").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"creature\":\"Marcel\",\"symptome\":\"x\"}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
