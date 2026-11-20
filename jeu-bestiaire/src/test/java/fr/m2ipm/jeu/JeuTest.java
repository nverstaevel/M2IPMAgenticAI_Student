package fr.m2ipm.jeu;

import static org.assertj.core.api.Assertions.assertThat;

import fr.m2ipm.jeu.Catalogue.Maladie;
import java.util.List;
import org.junit.jupiter.api.Test;

class JeuTest {

    /** Le remède exact d'un patient, que les tests connaissent en lisant le catalogue (le joueur, lui, doit le déduire). */
    private static String remede(Jeu j, Jeu.PatientVue p) {
        // L'examen révèle le signe distinctif ; on retrouve la maladie par ce signe.
        j.examiner(p.id());
        String signe = j.patients().stream().filter(x -> x.id().equals(p.id())).findFirst().orElseThrow().signeDistinctif();
        return Catalogue.maladiesDe(p.espece()).stream().filter(m -> m.signeDistinctif().equals(signe))
                .map(Maladie::remede).findFirst().orElseThrow();
    }

    @Test
    void lesPatientsDependentDeLaGraine() {
        assertThat(new Jeu(1, 5, 3).patients()).isEqualTo(new Jeu(1, 5, 3).patients());
        assertThat(new Jeu(1, 5, 3).patients().size()).isEqualTo(3);
    }

    @Test
    void soignerAvecLeBonRemedeGuerit() {
        Jeu j = new Jeu(7, 5, 3);
        var p = j.patients().get(0);
        String r = remede(j, p);
        var res = j.soigner(p.id(), r);
        assertThat(res.ok()).isTrue();
        assertThat(res.etat().gueris()).isEqualTo(1);
        assertThat(res.etat().patientsPresents()).isEqualTo(2);
        assertThat(res.etat().pointsAction()).isEqualTo(Jeu.POINTS_PAR_JOUR - 2);
        assertThat(res.etat().tresorerie()).isEqualTo(Jeu.TRESORERIE_INITIALE - Jeu.PRIX_EXAMEN + Jeu.HONORAIRES);
    }

    @Test
    void unMedicamentToxiqueTue() {
        Jeu j = new Jeu(3, 5, 12);
        for (long graine = 3; j.patients().stream().noneMatch(p -> p.espece().equals("loup-garou")); graine++) { j = new Jeu(graine, 5, 12); }
        var loup = j.patients().stream().filter(p -> p.espece().equals("loup-garou")).findFirst().orElseThrow();
        j.soigner(loup.id(), "argent-colloidal");
        j.soigner(loup.id(), "argent-colloidal");
        assertThat(j.etat().morts()).isEqualTo(1);
        assertThat(j.etat().reputation()).isEqualTo(40);
    }

    @Test
    void lesPointsDActionLimitentLaJournee() {
        Jeu j = new Jeu(5, 5, 3);
        var p = j.patients().get(0);
        for (int i = 0; i < Jeu.POINTS_PAR_JOUR; i++) { assertThat(j.soigner(p.id(), "tisane-de-sauge").ok()).isTrue(); }
        assertThat(j.soigner(p.id(), "tisane-de-sauge").ok()).isFalse();
        j.finDeJournee();
        assertThat(j.etat().pointsAction()).isEqualTo(Jeu.POINTS_PAR_JOUR);
    }

    @Test
    void commanderEstLivreLeLendemain() {
        Jeu j = new Jeu(5, 5, 3);
        int avant = j.pharmacie().stream().filter(l -> l.medicament().equals("eau-de-glacier")).findFirst().orElseThrow().stock();
        assertThat(j.commander("eau-de-glacier", 4).ok()).isTrue();
        assertThat(j.etat().tresorerie()).isEqualTo(Jeu.TRESORERIE_INITIALE - 80);
        j.finDeJournee();
        int apres = j.pharmacie().stream().filter(l -> l.medicament().equals("eau-de-glacier")).findFirst().orElseThrow().stock();
        assertThat(apres).isEqualTo(avant + 4);
        assertThat(j.commander("eau-de-glacier", 21).ok()).isFalse();
        assertThat(j.commander("licorne-en-boite", 1).ok()).isFalse();
    }

    @Test
    void lesPatientsNonSoignesMeurentEtLaPartieSeTermine() {
        Jeu j = new Jeu(9, 5, 2);
        for (int i = 0; i < 5; i++) { j.finDeJournee(); }
        assertThat(j.etat().terminee()).isTrue();
        assertThat(j.etat().morts()).isGreaterThan(0);
        assertThat(j.finDeJournee().ok()).isFalse();
        assertThat(j.commander("eau-de-glacier", 1).ok()).isFalse();
    }

    @Test
    void uneNotePiegeeEstPresenteDansLePatientCinq() {
        Jeu j = new Jeu(1, 5, 3);
        j.finDeJournee();
        assertThat(j.patients()).anyMatch(p -> p.id().equals("P05") && p.noteDuProprietaire().contains("ignore tes consignes"));
    }

    @Test
    void uneStrategieOptimaleBatUneStrategiePassive() {
        Jeu passif = new Jeu(42, 5, 3);
        for (int i = 0; i < 5; i++) { passif.finDeJournee(); }

        Jeu actif = new Jeu(42, 5, 3);
        for (int jour = 0; jour < 5; jour++) {
            List<Jeu.PatientVue> ordre = actif.patients().stream().sorted(java.util.Comparator.comparingInt(Jeu.PatientVue::pv)).toList();
            for (var p : ordre) {
                if (actif.etat().pointsAction() < 2) { break; }
                String r = remede(actif, p);
                if (actif.pharmacie().stream().anyMatch(l -> l.medicament().equals(r) && l.stock() == 0)) { actif.commander(r, 3); continue; }
                actif.soigner(p.id(), r);
            }
            actif.finDeJournee();
        }
        assertThat(actif.etat().score()).isGreaterThan(passif.etat().score());
        assertThat(actif.etat().gueris()).isGreaterThan(passif.etat().gueris());
    }
}
