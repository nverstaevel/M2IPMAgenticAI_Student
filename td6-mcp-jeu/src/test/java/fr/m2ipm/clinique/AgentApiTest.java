package fr.m2ipm.clinique;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
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

/** Agent complet (vrais outils, vraie base) avec le modèle simulé du profil « simule ». */
@SpringBootTest(properties = "agent.traces.dir=target/traces-test")
@ActiveProfiles("simule")
class AgentApiTest {

    @Autowired WebApplicationContext contexte;
    MockMvc mvc;

    @BeforeEach
    void init() {
        mvc = MockMvcBuilders.webAppContextSetup(contexte).apply(springSecurity()).build();
    }

    @Test
    @WithMockUser(roles = "PERSONNEL")
    void agentPuisTrace() throws Exception {
        String corps = mvc.perform(post("/api/agent").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"demande\":\"outil:rechercherProprietaires:{\\\"nom\\\":\\\"brume\\\"}\",\"maxEtapes\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultat.motif").value("REPONSE_FINALE"))
                .andExpect(jsonPath("$.resultat.etapes.length()").value(2))
                .andExpect(jsonPath("$.resultat.etapes[0].outil").value("rechercherProprietaires"))
                .andExpect(jsonPath("$.resultat.etapes[0].resultat").value(org.hamcrest.Matchers.containsString("Brumevent")))
                .andReturn().getResponse().getContentAsString();
        String runId = JsonPath.read(corps, "$.runId");
        mvc.perform(get("/api/agent/" + runId + "/trace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/agent/inconnu/trace")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "LECTURE")
    void lAgentEstReserveAuPersonnel() throws Exception {
        mvc.perform(post("/api/agent").contentType(MediaType.APPLICATION_JSON).content("{\"demande\":\"bonjour\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/agent/x/trace")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PERSONNEL")
    void demandeVideRefusee() throws Exception {
        mvc.perform(post("/api/agent").contentType(MediaType.APPLICATION_JSON).content("{\"demande\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PERSONNEL")
    void bouclesDuFrameworkRepondSansTrace() throws Exception {
        mvc.perform(post("/api/agent/framework").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"demande\":\"outil:rechercherProprietaires:{\\\"nom\\\":\\\"brume\\\"}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reponse").value(org.hamcrest.Matchers.containsString("Brumevent")));
    }
}
