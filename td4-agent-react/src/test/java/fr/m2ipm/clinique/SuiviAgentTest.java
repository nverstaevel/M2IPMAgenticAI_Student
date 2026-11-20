package fr.m2ipm.clinique;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import fr.m2ipm.clinique.agent.*;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Le graphe en direct : événements émis par la boucle ReAct, et droits d'accès à la page et à l'API de suivi. */
@SpringBootTest(properties = "agent.traces.dir=target/traces-test")
@ActiveProfiles("simule")
class SuiviAgentTest {

    @Autowired WebApplicationContext contexte;

    @Test
    void laBoucleEmetUnNoeudParAppelDeModeleEtParOutil() throws Exception {
        var suivi = new SuiviExecution();
        var m = new ModeleScenario().appel("Je cherche.", "echo", "{\"texte\":\"a\"}").finale("Voilà.");
        var agent = new AgentReactService(m, ToolCallbacks.from(new OutilsBidon()),
                new TraceStore(Files.createTempDirectory("traces")), suivi, 100000);
        agent.executer("run-suivi", "demande", 5);

        List<Evenement> evts = suivi.evenements("run-suivi").orElseThrow();
        assertThat(evts.get(0).type()).isEqualTo("demarrage");
        assertThat(evts.get(evts.size() - 1).type()).isEqualTo("fin");
        assertThat(evts.get(evts.size() - 1).statut()).isEqualTo("REPONSE_FINALE");
        assertThat(evts).extracting(Evenement::id).contains("demande", "e1", "e1-outil", "e2", "fin");
        Evenement outil = evts.stream().filter(e -> "e1-outil".equals(e.id()) && "OK".equals(e.statut())).findFirst().orElseThrow();
        assertThat(outil.resultat()).contains("echo:a");
        Evenement creation = evts.stream().filter(e -> "e2".equals(e.id())).findFirst().orElseThrow();
        assertThat(creation.parents()).containsExactly("e1-outil");
        assertThat(suivi.liste()).extracting(SuiviExecution.Resume::runId).contains("run-suivi");
        assertThat(suivi.evenements("inconnu")).isEmpty();
    }

    @Test
    void uneErreurDuModeleEstVisibleDansLeGraphe() throws Exception {
        var suivi = new SuiviExecution();
        var m = new org.springframework.ai.chat.model.ChatModel() {
            @Override
            public org.springframework.ai.chat.model.ChatResponse call(org.springframework.ai.chat.prompt.Prompt p) {
                throw new IllegalStateException("503 modèle surchargé");
            }
        };
        var agent = new AgentReactService(m, ToolCallbacks.from(new OutilsBidon()),
                new TraceStore(Files.createTempDirectory("traces")), suivi, 100000);
        var r = agent.executer("run-erreur", "demande", 5);
        assertThat(r.motif()).isEqualTo(MotifArret.ERREUR);
        List<Evenement> evts = suivi.evenements("run-erreur").orElseThrow();
        assertThat(evts).anyMatch(e -> "e1".equals(e.id()) && "ERREUR".equals(e.statut()) && e.resultat().contains("503"));
        assertThat(evts.get(evts.size() - 1).statut()).isEqualTo("ERREUR");
    }

    @Test
    @WithMockUser(roles = "PERSONNEL")
    void lancerPuisSuivreLExecution() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(contexte).apply(springSecurity()).build();
        mvc.perform(get("/agents")).andExpect(status().isOk());
        String corps = mvc.perform(post("/api/agent/lancer").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"demande\":\"outil:rechercherProprietaires:{\\\"nom\\\":\\\"brume\\\"}\",\"maxEtapes\":5}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String runId = JsonPath.read(corps, "$.runId");
        String dernier = "";
        for (int i = 0; i < 100 && !dernier.contains("\"type\":\"fin\""); i++) {
            Thread.sleep(100);
            dernier = mvc.perform(get("/api/agent/suivi/" + runId)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
        }
        assertThat(dernier).contains("\"type\":\"fin\"").contains("REPONSE_FINALE").contains("Brumevent");
        mvc.perform(get("/api/agent/suivi")).andExpect(status().isOk());
        mvc.perform(get("/api/agent/suivi/inconnu")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "LECTURE")
    void lePersonnelSeulVoitLeSuivi() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(contexte).apply(springSecurity()).build();
        mvc.perform(get("/agents")).andExpect(status().isForbidden());
        mvc.perform(get("/api/agent/suivi")).andExpect(status().isForbidden());
        mvc.perform(post("/api/agent/lancer").contentType(MediaType.APPLICATION_JSON).content("{\"demande\":\"x\"}"))
                .andExpect(status().isForbidden());
    }
}
