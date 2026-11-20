package fr.m2ipm.clinique;

import static org.assertj.core.api.Assertions.assertThat;

import fr.m2ipm.clinique.roles.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

/** La boucle actor-critic est testée sans Spring : deux modèles scriptés, un pour chaque rôle. */
class DuoServiceTest {

    static final String DIAG = "{\"creature\":\"Lunatique\",\"diagnostic\":\"Insomnie\",\"traitements\":[\"%s\"],\"justification\":\"j\"}";
    static final String APPROUVE = "{\"verdict\":\"APPROUVE\",\"objections\":[],\"sources\":[\"https://fr.wikipedia.org/wiki/Loup-garou\"]}";
    static final String REJETE = "{\"verdict\":\"REJETE\",\"objections\":[\"L'argent est nocif\"],\"sources\":[\"https://fr.wikipedia.org/wiki/Loup-garou\"]}";
    static final String DOUTE = "{\"verdict\":\"DOUTE\",\"objections\":[\"Sources maigres\"],\"sources\":[]}";

    static final CasBestiaire CAS = new CasBestiaire(1, "Lunatique", "Loup-garou", "Clairdelune", "insomnie",
            "compléments à base d'argent", Verdict.REJETE);

    private DuoService duo(ModeleTexte veto, ModeleTexte gardien) {
        return new DuoService(new VeterinaireAgent(ChatClient.create(veto)), new GardienDuBestiaire(ChatClient.create(gardien)),
                java.time.Clock.systemUTC(), "test");
    }

    @Test
    void sArreteSurApprouve() {
        var veto = new ModeleTexte(DIAG.formatted("tisane"));
        var run = duo(veto, new ModeleTexte(APPROUVE)).traiter(CAS, ProfilActeur.PRUDENT);
        assertThat(run.statut()).isEqualTo(StatutDuo.APPROUVEE);
        assertThat(run.tours()).hasSize(1);
        assertThat(run.validation()).as("jamais validé automatiquement").isEqualTo(Validation.EN_ATTENTE_DE_VALIDATION);
    }

    @Test
    void sArreteSurDouteEtRenvoieVersUnHumain() {
        var run = duo(new ModeleTexte(DIAG.formatted("sauna")), new ModeleTexte(DOUTE)).traiter(CAS, ProfilActeur.PRESSE);
        assertThat(run.statut()).isEqualTo(StatutDuo.VERS_HUMAIN);
        assertThat(run.tours()).hasSize(1);
    }

    @Test
    void sArreteAuPlafondDeTroisToursSiLeGardienRejetteToujours() {
        var veto = new ModeleTexte(DIAG.formatted("argent"));
        var run = duo(veto, new ModeleTexte(REJETE)).traiter(CAS, ProfilActeur.PRESSE);
        assertThat(run.statut()).isEqualTo(StatutDuo.NON_CONVERGENTE);
        assertThat(run.tours()).hasSize(DuoService.MAX_TOURS);
        assertThat(veto.recus).hasSize(3);
    }

    @Test
    void lesObjectionsSontTransmisesAuVeterinaireAuTourSuivant() {
        var veto = new ModeleTexte(DIAG.formatted("argent"), DIAG.formatted("tisane"));
        var run = duo(veto, new ModeleTexte(REJETE, APPROUVE)).traiter(CAS, ProfilActeur.PRESSE);
        assertThat(run.statut()).isEqualTo(StatutDuo.APPROUVEE);
        assertThat(run.tours()).hasSize(2);
        assertThat(veto.recus.get(0).getContents()).doesNotContain("Objections du gardien");
        assertThat(veto.dernierUtilisateur()).contains("Objections du gardien").contains("L'argent est nocif");
    }

    @Test
    void lePresseRecoitLaPisteHabituelleEtLePrudentNon() {
        var presse = new ModeleTexte(DIAG.formatted("x"));
        duo(presse, new ModeleTexte(APPROUVE)).traiter(CAS, ProfilActeur.PRESSE);
        assertThat(presse.dernierUtilisateur()).contains("Piste habituelle : compléments à base d'argent");
        var prudent = new ModeleTexte(DIAG.formatted("x"));
        duo(prudent, new ModeleTexte(APPROUVE)).traiter(CAS, ProfilActeur.PRUDENT);
        assertThat(prudent.dernierUtilisateur()).doesNotContain("Piste habituelle");
    }

    @Test
    void unDossierNeSeValideQu_uneFois() {
        var run = duo(new ModeleTexte(DIAG.formatted("x")), new ModeleTexte(APPROUVE)).traiter(CAS, ProfilActeur.PRUDENT);
        run.decider(true, "gribouille", java.time.Instant.now());
        assertThat(run.validation()).isEqualTo(Validation.VALIDE);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> run.decider(false, "autre", java.time.Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }
}
