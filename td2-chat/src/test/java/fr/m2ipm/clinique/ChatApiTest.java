package fr.m2ipm.clinique;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

/** Chaîne complète (contrôleur, ChatClient, mémoire JDBC, outils) avec le modèle simulé. */
@SpringBootTest
@ActiveProfiles("simule")
class ChatApiTest {

    @Autowired WebApplicationContext contexte;
    MockMvc mvc;

    @BeforeEach
    void init() {
        mvc = MockMvcBuilders.webAppContextSetup(contexte).apply(springSecurity()).build();
    }

    private static String chat(String conversation, String message) {
        return "{\"conversationId\":\"" + conversation + "\",\"message\":\"" + message.replace("\"", "\\\"") + "\"}";
    }

    @Test
    @WithMockUser(roles = "LECTURE")
    void leVisiteurPeutDiscuter() throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content(chat("c1", "Bonjour")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reponse").value("Simulé : Bonjour"));
    }

    @Test
    @WithMockUser(roles = "LECTURE")
    void unAppelDOutilPasseParLaBoucleDOutils() throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content(chat("c2", "outil:rechercherProprietaires:{\"nom\":\"brume\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reponse").value(org.hamcrest.Matchers.containsString("Brumevent")))
                .andExpect(jsonPath("$.reponse").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("0600000001"))));
    }

    @Test
    @WithMockUser(roles = "LECTURE")
    void triageRenvoieUneStructure() throws Exception {
        mvc.perform(post("/api/triage").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texte\":\"Mon dragon éternue de la fumée verte\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.espece").value("Dragon"))
                .andExpect(jsonPath("$.urgence").value("MOYENNE"))
                .andExpect(jsonPath("$.symptomes[0]").value("fumée verdâtre"));
    }

    @Test
    @WithMockUser(roles = "PERSONNEL")
    void propositionPuisConfirmationParLePersonnel() throws Exception {
        // Dr Gribouille (soigneur 1) pour Paillette (créature 2, licorne), créneau libre.
        String appel = "outil:proposerConsultation:{\"creatureId\":2,\"soigneurId\":1,"
                + "\"dateHeure\":\"2032-06-01T09:00:00\",\"motif\":\"Sabot\"}";
        String corps = mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content(chat("c3", appel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propositions[0].creatureNom").value("Paillette"))
                .andReturn().getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(corps, "$.propositions[0].id");

        mvc.perform(get("/api/creatures/2/consultations")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(post("/api/propositions/" + id + "/confirmer")).andExpect(status().isOk());
        mvc.perform(get("/api/creatures/2/consultations")).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "LECTURE")
    void leVisiteurNePeutPasConfirmer() throws Exception {
        mvc.perform(post("/api/propositions/n-importe-quoi/confirmer")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LECTURE")
    void effacerLeContexteVideLaMemoireDeLaConversation(@Autowired org.springframework.ai.chat.memory.ChatMemory memoire) throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content(chat("c-effacer", "Bonjour")))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertFalse(memoire.get("c-effacer").isEmpty());
        mvc.perform(delete("/api/chat/c-effacer")).andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertTrue(memoire.get("c-effacer").isEmpty());
    }
}
