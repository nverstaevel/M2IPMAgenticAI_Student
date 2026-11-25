package fr.m2ipm.clinique.jeu;

import static org.assertj.core.api.Assertions.assertThat;

import fr.m2ipm.clinique.jeu.GardienRegles.FicheVue;
import fr.m2ipm.clinique.jeu.GardienRegles.MaladieVue;
import fr.m2ipm.clinique.jeu.GardienRegles.PatientVu;
import fr.m2ipm.clinique.scenario.Decision;
import fr.m2ipm.clinique.scenario.Resultat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Le gardien à règles se teste sans jeu ni modèle : des patients, une fiche et un stock écrits à la main. */
class GardienReglesTest {

    static final Map<String, FicheVue> FICHES = Map.of("vampire", new FicheVue(
            List.of(new MaladieVue("dents ternes", "sang-de-synthese"), new MaladieVue("peau grise", "creme-d-ombre")),
            Set.of("gousse-d-ail")));
    static final Map<String, Integer> STOCK = Map.of("sang-de-synthese", 2, "creme-d-ombre", 0, "gousse-d-ail", 3, "tisane-de-sauge", 5);

    Resultat verifier(String plan, Map<String, PatientVu> patients, int points) {
        return GardienRegles.verifier(plan, patients, FICHES, STOCK, points);
    }

    @Test
    void lesLignesDuPlanSontLuesEtLeResteIgnore() {
        assertThat(GardienRegles.lignes("Plan :\nP03 : Sang-de-synthese (urgent)\n- p04: creme-d-ombre\nVerdict : APPROUVE"))
                .containsExactly(new GardienRegles.Ligne("P03", "sang-de-synthese"), new GardienRegles.Ligne("P04", "creme-d-ombre"));
    }

    @Test
    void unBonRemedeSurUnPatientExamineEstApprouve() {
        Resultat r = verifier("P01 : sang-de-synthese (dents ternes)", Map.of("P01", new PatientVu("P01", "vampire", "dents ternes")), 5);
        assertThat(r.decision()).isEqualTo(Decision.APPROUVE);
        assertThat(r.texte()).contains("Verdict du gardien : APPROUVE");
    }

    @Test
    void unMedicamentToxiqueEstRejete() {
        Resultat r = verifier("P01 : gousse-d-ail", Map.of("P01", new PatientVu("P01", "vampire", null)), 5);
        assertThat(r.decision()).isEqualTo(Decision.REJETE);
        assertThat(r.remarques()).anyMatch(s -> s.contains("TOXIQUE"));
    }

    @Test
    void leRemedeDeLAutreMaladieEstRejeteQuandLeSigneEstConnu() {
        Resultat r = verifier("P01 : creme-d-ombre", Map.of("P01", new PatientVu("P01", "vampire", "dents ternes")), 5);
        assertThat(r.decision()).isEqualTo(Decision.REJETE);
        assertThat(r.remarques()).anyMatch(s -> s.contains("sang-de-synthese"));
    }

    @Test
    void unPatientNonExamineEstUnPariEtDonneDoute() {
        Resultat r = verifier("P01 : sang-de-synthese", Map.of("P01", new PatientVu("P01", "vampire", null)), 5);
        assertThat(r.decision()).isEqualTo(Decision.DOUTE);
    }

    @Test
    void unMedicamentSansEffetSurLEspeceEstRejete() {
        Resultat r = verifier("P01 : tisane-de-sauge", Map.of("P01", new PatientVu("P01", "vampire", null)), 5);
        assertThat(r.decision()).isEqualTo(Decision.REJETE);
    }

    @Test
    void ilFautAssezDePointsEtUnPatientExistant() {
        Map<String, PatientVu> p = Map.of("P01", new PatientVu("P01", "vampire", "dents ternes"));
        assertThat(verifier("P01 : sang-de-synthese\nP02 : sang-de-synthese", p, 1).decision()).isEqualTo(Decision.REJETE);
        assertThat(verifier("P09 : sang-de-synthese", p, 5).remarques()).anyMatch(s -> s.contains("patient inconnu"));
    }

    @Test
    void unPlanIllisibleEstRejeteMaisSansPatientIlEstApprouve() {
        assertThat(verifier("je soignerais tout le monde", Map.of("P01", new PatientVu("P01", "vampire", null)), 5).decision()).isEqualTo(Decision.REJETE);
        assertThat(verifier("rien à faire", Map.of(), 5).decision()).isEqualTo(Decision.APPROUVE);
    }

    @Test
    void uneRuptureDeStockEstSignaleeSansRejet() {
        Resultat r = verifier("P01 : creme-d-ombre", Map.of("P01", new PatientVu("P01", "vampire", "peau grise")), 5);
        assertThat(r.decision()).isEqualTo(Decision.DOUTE);
        assertThat(r.remarques()).anyMatch(s -> s.contains("Stock"));
    }
}
