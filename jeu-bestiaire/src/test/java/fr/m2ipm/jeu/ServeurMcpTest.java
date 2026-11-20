package fr.m2ipm.jeu;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Le contexte démarre avec le serveur MCP, et les huit outils du jeu sont publiés. */
@SpringBootTest
class ServeurMcpTest {

    @Autowired
    List<ToolCallbackProvider> fournisseurs;

    @Test
    void lesOutilsDuJeuSontPublies() {
        List<String> noms = fournisseurs.stream().flatMap(f -> Arrays.stream(f.getToolCallbacks()))
                .map(c -> c.getToolDefinition().name()).toList();
        assertThat(noms).contains("etat_clinique", "lister_patients", "fiche_espece", "consulter_pharmacie",
                "examiner_patient", "soigner", "commander", "fin_de_journee");
    }
}
