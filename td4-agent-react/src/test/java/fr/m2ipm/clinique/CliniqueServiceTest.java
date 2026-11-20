package fr.m2ipm.clinique;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.m2ipm.clinique.dto.*;
import fr.m2ipm.clinique.service.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("simule")
@Transactional
class CliniqueServiceTest {

    @Autowired CliniqueService service;

    private Long idCreature(String nomProprietaire, String nomCreature) {
        Long pid = service.rechercherProprietaires(nomProprietaire).get(0).id();
        return service.creaturesDuProprietaire(pid).stream()
                .filter(c -> c.nom().equals(nomCreature)).findFirst().orElseThrow().id();
    }

    private Long idSoigneur(String espece, String nom) {
        return service.soigneursPourEspece(espece).stream()
                .filter(s -> s.nom().equals(nom)).findFirst().orElseThrow().id();
    }

    @Test
    void rechercheEstPartielleEtInsensibleALaCasse() {
        assertThat(service.rechercherProprietaires("BRUME")).extracting(ProprietaireDto::nom)
                .containsExactly("Brumevent");
        assertThat(service.rechercherProprietaires("")).hasSize(3);
    }

    @Test
    void unProprietaireRetrouveSesCreatures() {
        Long pid = service.rechercherProprietaires("Fumerolle").get(0).id();
        assertThat(service.creaturesDuProprietaire(pid)).extracting(CreatureDto::nom)
                .containsExactly("Flammèche", "Grognon");
    }

    @Test
    void messageDErreurLisible() {
        assertThatThrownBy(() -> service.trouverProprietaire(4242L))
                .isInstanceOf(RessourceIntrouvableException.class)
                .hasMessage("Aucun propriétaire d'identifiant 4242");
    }

    @Test
    void historiqueDuPlusRecentAuPlusAncien() {
        Long dragon = idCreature("Fumerolle", "Flammèche");
        Long gribouille = idSoigneur("Dragon", "Dr Gribouille");
        service.planifierConsultation(dragon, gribouille, LocalDateTime.of(2026, 6, 1, 9, 0), "Contrôle");
        var historique = service.consultationsDeLaCreature(dragon);
        assertThat(historique).hasSize(2);
        assertThat(historique.get(0).dateHeure()).isAfter(historique.get(1).dateHeure());
    }

    @Test
    void chevauchementRefuseMaisCreneauxAdjacentsAcceptes() {
        Long dragon = idCreature("Fumerolle", "Flammèche");
        Long gribouille = idSoigneur("Dragon", "Dr Gribouille");
        LocalDateTime t = LocalDateTime.of(2026, 7, 1, 10, 0);
        service.planifierConsultation(dragon, gribouille, t, "Contrôle");

        assertThatThrownBy(() -> service.planifierConsultation(dragon, gribouille, t.plusMinutes(15), "Doublon"))
                .isInstanceOf(ConflitDeCreneauException.class)
                .hasMessageContaining("est déjà pris le 01/07 à 10h15");
        // 30 minutes plus tard, avant ou après : le créneau est libre.
        service.planifierConsultation(dragon, gribouille, t.plusMinutes(30), "Suite");
        service.planifierConsultation(dragon, gribouille, t.minusMinutes(30), "Avant");
    }

    @Test
    void diagnosticEnregistreUneSeuleFois() {
        Long rebelote = idCreature("Brumevent", "Rebelote");
        Long consultation = service.consultationsDeLaCreature(rebelote).get(0).id();
        service.enregistrerDiagnostic(consultation, "Combustion répétée", "Cendres de repos");
        assertThat(service.consultationsDeLaCreature(rebelote).get(0).diagnostic()).isEqualTo("Combustion répétée");
        assertThatThrownBy(() -> service.enregistrerDiagnostic(consultation, "x", "y"))
                .isInstanceOf(OperationInvalideException.class);
    }
}
