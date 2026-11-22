package fr.m2ipm.clinique.jeu;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Vraie liaison MCP : à lancer avec le serveur du jeu démarré, {@code mvn verify -Djeu.reel=true} (la CI le fait).
 * Le profil « jeu » connecte le client du starter MCP ; on découvre les outils puis on appelle {@code etat_clinique}.
 */
@SpringBootTest(properties = "agent.traces.dir=target/traces-test")
@ActiveProfiles({"simule", "jeu"})
@EnabledIfSystemProperty(named = "jeu.reel", matches = "true")
class LiaisonMcpTest {

    @Autowired JeuMcp jeu;

    @Test
    void lesOutilsDuJeuSontDecouvertsEtAppelables() {
        var noms = java.util.Arrays.stream(jeu.outils()).map(o -> o.getToolDefinition().name()).toList();
        assertThat(noms).contains("etat_clinique", "lister_patients", "fiche_espece", "carnet_soins", "consulter_pharmacie",
                "examiner_patient", "soigner", "commander", "fin_de_journee");
        JeuMcp.Etat etat = jeu.etat();
        assertThat(etat.jour()).isEqualTo(1);
        assertThat(etat.pointsAction()).isEqualTo(5);
        assertThat(etat.terminee()).isFalse();
        // Une action : examiner un patient coûte un point d'action et 5 de trésorerie.
        String patients = jeu.appeler("lister_patients", "{}");
        assertThat(patients).contains("P01");
        jeu.appeler("examiner_patient", "{\"idPatient\":\"P01\"}");
        assertThat(jeu.etat().pointsAction()).isEqualTo(4);
        assertThat(jeu.appeler("fin_de_journee", "{}")).contains("Jour 2");
    }
}
