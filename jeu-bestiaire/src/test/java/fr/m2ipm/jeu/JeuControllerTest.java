package fr.m2ipm.jeu;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JeuControllerTest {

    @Test
    void lHumainPeutJouerALaMainParLeControleur() {
        JeuController c = new JeuController(new PartieService(1, 5, 3));
        String id = c.partie().get("patients") instanceof java.util.List<?> l ? ((Jeu.PatientVue) l.get(0)).id() : null;
        assertThat(c.examiner(id).ok()).isTrue();
        assertThat(c.soigner(id, "tisane-de-sauge").ok()).isTrue();
        assertThat(c.commander("tisane-de-sauge", 2).ok()).isTrue();
        assertThat(c.finDeJournee().etat().jour()).isEqualTo(2);
    }
}
