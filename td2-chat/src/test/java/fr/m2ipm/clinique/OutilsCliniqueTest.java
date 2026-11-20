package fr.m2ipm.clinique;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.m2ipm.clinique.ai.*;
import fr.m2ipm.clinique.service.ConflitDeCreneauException;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Un test par outil : la méthode est appelée directement, sans modèle. */
@SpringBootTest
@ActiveProfiles("simule")
@Transactional
class OutilsCliniqueTest {

    @Autowired OutilsClinique outils;

    private static ToolContext contexte() {
        return new ToolContext(Map.of("conversationId", "test"));
    }

    @Test
    void rechercherProprietairesNeRenvoiePasLeTelephone() {
        var resultat = outils.rechercherProprietaires("brume");
        assertThat(resultat).extracting(ProprietaireVue::nom).containsExactly("Brumevent");
        assertThat(ProprietaireVue.class.getRecordComponents()).extracting("name").doesNotContain("telephone");
    }

    @Test
    void trouverProprietaire() {
        Long id = outils.rechercherProprietaires("fumerolle").get(0).id();
        assertThat(outils.trouverProprietaire(id).prenom()).isEqualTo("Gaspard");
    }

    @Test
    void creaturesDuProprietaire() {
        Long id = outils.rechercherProprietaires("clairdelune").get(0).id();
        assertThat(outils.creaturesDuProprietaire(id)).extracting(c -> c.nom())
                .containsExactly("Lunatique", "Monsieur Crépuscule");
    }

    @Test
    void consultationsDeLaCreature() {
        Long fumerolle = outils.rechercherProprietaires("fumerolle").get(0).id();
        Long flammeche = outils.creaturesDuProprietaire(fumerolle).stream()
                .filter(c -> c.nom().equals("Flammèche")).findFirst().orElseThrow().id();
        assertThat(outils.consultationsDeLaCreature(flammeche)).hasSize(1);
    }

    @Test
    void soigneursPourEspece() {
        assertThat(outils.soigneursPourEspece("Phénix")).extracting(s -> s.nom()).containsExactly("Dr Gribouille");
        assertThat(outils.soigneursPourEspece("Griffon")).isEmpty();
    }

    @Test
    void proposerNEcritRienEtDateIllisibleEstExpliquee() {
        Long fumerolle = outils.rechercherProprietaires("fumerolle").get(0).id();
        Long creature = outils.creaturesDuProprietaire(fumerolle).get(0).id();
        Long soigneur = outils.soigneursPourEspece("Dragon").get(0).id();

        String resume = outils.proposerConsultation(creature, soigneur, "2031-05-01T09:00:00", "Contrôle", contexte());
        assertThat(resume).contains("pas encore confirmée");
        assertThat(outils.consultationsDeLaCreature(creature)).hasSize(1); // toujours la seule consultation du jeu de données

        assertThat(outils.proposerConsultation(creature, soigneur, "demain matin", "Contrôle", contexte()))
                .contains("Date illisible");
    }

    @Test
    void proposerRefuseUnCreneauDejaPris() {
        Long brumevent = outils.rechercherProprietaires("brumevent").get(0).id();
        Long rebelote = outils.creaturesDuProprietaire(brumevent).stream()
                .filter(c -> c.nom().equals("Rebelote")).findFirst().orElseThrow().id();
        Long gribouille = outils.soigneursPourEspece("Phénix").get(0).id();
        // Le jeu de données place déjà un rendez-vous du Dr Gribouille le 12/03/2026 à 14 h.
        assertThatThrownBy(() -> outils.proposerConsultation(rebelote, gribouille, "2026-03-12T14:10:00", "Bis", contexte()))
                .isInstanceOf(ConflitDeCreneauException.class)
                .hasMessageContaining("est déjà pris le 12/03 à 14h10");
    }
}
