package fr.m2ipm.clinique;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.m2ipm.clinique.ai.OutilsClinique;
import fr.m2ipm.clinique.externe.*;
import fr.m2ipm.clinique.service.ConflitDeCreneauException;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Un test par outil externe (sauf Wikipédia, couvert par WikipediaClientTest sans réseau). */
@SpringBootTest
@ActiveProfiles("simule")
class OutilsExternesTest {

    @Autowired OutilsExternes outils;
    @Autowired OutilsClinique clinique;
    @Autowired AgendaPort agenda;
    @Autowired PropositionAgendaService propositions;

    private static ToolContext contexte() {
        return new ToolContext(Map.of("conversationId", "test-agenda"));
    }

    private Long flammecheId() {
        Long fumerolle = clinique.rechercherProprietaires("fumerolle").get(0).id();
        return clinique.creaturesDuProprietaire(fumerolle).stream()
                .filter(c -> c.nom().equals("Flammèche")).findFirst().orElseThrow().id();
    }

    @Test
    void consulterTarifLitLeClasseurExcel() {
        assertThat(outils.consulterTarif("Consultation", "Dragon")).contains("80,00").contains("Dragon");
        assertThat(outils.consulterTarif("consultation", "dragon")).contains("80,00");
        assertThat(outils.consulterTarif("Exorcisme", "Dragon")).contains("Aucun tarif");
    }

    @Test
    void stockPharmacie() {
        assertThat(outils.stockPharmacie("Baume ignifuge")).contains("12").contains("pots");
        assertThat(outils.stockPharmacie("poudre de lune")).contains("rupture");
        assertThat(outils.stockPharmacie("Elixir de valeriane")).contains("7");
        assertThat(outils.stockPharmacie("Potion inconnue")).contains("inconnu");
    }

    @Test
    void chercherMedicamentInterrogeLaSecondeBase() {
        String fiche = outils.chercherMedicament("baume", "Dragon");
        assertThat(fiche).contains("aloès de cendre").contains("Yéti").contains("1 noix");
        assertThat(outils.chercherMedicament("baume", "Vampire")).contains("Aucun médicament");
    }

    @Test
    void chercherMedicamentResisteAUneInjectionSql() {
        assertThat(outils.chercherMedicament("'; DROP TABLE medicament; --", "Dragon")).contains("Aucun médicament");
        assertThat(outils.chercherMedicament("baume", "Dragon")).contains("aloès de cendre");
    }

    @Test
    void creneauxLibresExcluentLesCreneauxOccupes() {
        String libres = outils.creneauxLibres("2026-12-03");
        assertThat(libres).contains("09:00").doesNotContain("10:00").doesNotContain("14:30");
        assertThat(outils.creneauxLibres("pas-une-date")).contains("illisible");
    }

    @Test
    void proposerEvenementAgendaNEcritRienAvantConfirmation() {
        AgendaSimule simule = (AgendaSimule) agenda;
        int avant = simule.evenementsCrees().size();
        String reponse = outils.proposerEvenementAgenda(flammecheId(), "2026-12-04T11:00:00", "Rhume", contexte());
        assertThat(reponse).contains("pas encore créée").contains("Consultation — Flammèche (Dragon)");
        assertThat(simule.evenementsCrees()).hasSize(avant);

        var proposition = propositions.enAttente("test-agenda").stream()
                .filter(p -> p.dateHeure().equals(LocalDateTime.of(2026, 12, 4, 11, 0))).findFirst().orElseThrow();
        String id = propositions.confirmer(proposition.id());
        assertThat(simule.evenementsCrees()).hasSize(avant + 1);
        assertThat(simule.evenementsCrees().get(avant).titre()).isEqualTo("Consultation — Flammèche (Dragon)");
        assertThat(id).startsWith("sim-");
        assertThat(agenda.creneauxLibres(proposition.dateHeure().toLocalDate())).doesNotContain(proposition.dateHeure().toLocalTime());
    }

    @Test
    void proposerSurUnCreneauOccupeEstRefuse() {
        assertThat(outils.proposerEvenementAgenda(flammecheId(), "2026-12-03T10:00:00", "Rhume", contexte()))
                .contains("Impossible").contains("pas libre");
        assertThatThrownBy(() -> propositions.proposer("c", flammecheId(), LocalDateTime.of(2026, 12, 3, 10, 0), "x"))
                .isInstanceOf(ConflitDeCreneauException.class);
    }
}
