package fr.m2ipm.clinique;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("simule")
class ApiTest {

    @Autowired WebApplicationContext contexte;
    MockMvc mvc;

    @BeforeEach
    void init() {
        mvc = MockMvcBuilders.webAppContextSetup(contexte).apply(springSecurity()).build();
    }

    @Test
    void anonymeNeVoitRien() throws Exception {
        mvc.perform(get("/api/proprietaires")).andExpect(result -> {
            int code = result.getResponse().getStatus();
            org.junit.jupiter.api.Assertions.assertTrue(code == 302 || code == 401, "statut inattendu : " + code);
        });
    }

    @Test
    @WithMockUser(roles = "LECTURE")
    void lectureAutoriseePourLeVisiteur() throws Exception {
        mvc.perform(get("/api/proprietaires").param("nom", "brume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("Brumevent"));
    }

    @Test
    @WithMockUser(roles = "LECTURE")
    void ecritureInterditePourLeVisiteur() throws Exception {
        mvc.perform(post("/api/consultations").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"creatureId\":1,\"soigneurId\":1,\"dateHeure\":\"2026-09-01T10:00:00\",\"motif\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PERSONNEL")
    void erreur404LisibleEtErreur400Validation() throws Exception {
        mvc.perform(get("/api/proprietaires/4242"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Aucun propriétaire d'identifiant 4242"));
        mvc.perform(post("/api/consultations").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PERSONNEL")
    void planificationRenvoie201PuisConflit409() throws Exception {
        String corps = "{\"creatureId\":3,\"soigneurId\":2,\"dateHeure\":\"2030-01-01T10:00:00\",\"motif\":\"Contrôle\"}";
        mvc.perform(post("/api/consultations").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/consultations").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isConflict());
    }
}
