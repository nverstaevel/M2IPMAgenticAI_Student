package fr.m2ipm.clinique;

import static org.assertj.core.api.Assertions.assertThat;

import fr.m2ipm.clinique.experience.ExperienceContexte;
import fr.m2ipm.clinique.experience.Mesure;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** L'expérience est testée avec un « modèle » qui recopie son prompt : il ne répond juste que si le contexte contient la réponse. */
class ExperienceContexteTest {

    private final ExperienceContexte experience = new ExperienceContexte(prompt -> prompt);

    @Test
    void dixQuestionsDeReference() {
        assertThat(experience.questions()).hasSize(10);
    }

    @Test
    void rechercheRenvoieAuPlusTroisPassagesCourts() {
        List<String> passages = experience.chercherDansBestiaire("phénix éclot température œuf");
        assertThat(passages).isNotEmpty().hasSizeLessThanOrEqualTo(3);
        assertThat(passages).allSatisfy(p -> assertThat(p.length()).isLessThanOrEqualTo(800));
        assertThat(passages.get(0)).contains("412");
    }

    @Test
    void strategieBRetrouveLesFaitsLa_ouLaStrategieATronqueeEchoue() throws Exception {
        Path csv = Path.of("target/mesures-test.csv");
        List<Mesure> mesures = experience.lancer(5000, csv);
        int exactA = mesures.stream().filter(m -> m.strategie().equals("A")).mapToInt(Mesure::exactitude).sum();
        int exactB = mesures.stream().filter(m -> m.strategie().equals("B")).mapToInt(Mesure::exactitude).sum();
        assertThat(exactB).isGreaterThanOrEqualTo(8).isGreaterThan(exactA);
        long tokensA = mesures.stream().filter(m -> m.strategie().equals("A")).mapToInt(Mesure::jetonsEntree).sum();
        long tokensB = mesures.stream().filter(m -> m.strategie().equals("B")).mapToInt(Mesure::jetonsEntree).sum();
        assertThat(tokensB).isLessThan(tokensA);
        assertThat(Files.readAllLines(csv)).hasSize(21).first().asString().startsWith("question;strategie");
    }

    @Test
    void variationsDePositionEtDeBruit() {
        var q = experience.questions().get(0);
        for (String position : List.of("debut", "milieu", "fin")) {
            for (String bruit : List.of("aucun", "moitie", "double")) {
                assertThat(experience.avecVariation(q, position, bruit).exactitude()).isEqualTo(1);
            }
        }
        assertThat(experience.avecVariation(q, "milieu", "double").jetonsEntree())
                .isGreaterThan(experience.avecVariation(q, "milieu", "aucun").jetonsEntree());
    }
}
