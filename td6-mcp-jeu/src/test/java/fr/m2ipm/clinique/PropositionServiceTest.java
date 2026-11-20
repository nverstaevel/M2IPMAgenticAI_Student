package fr.m2ipm.clinique;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.m2ipm.clinique.ai.PropositionService;
import fr.m2ipm.clinique.service.CliniqueService;
import fr.m2ipm.clinique.service.OperationInvalideException;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("simule")
@Transactional
class PropositionServiceTest {

    /** Horloge réglable pour tester l'expiration sans attendre. */
    static class HorlogeReglable extends Clock {
        private Instant t = Instant.parse("2031-01-01T10:00:00Z");
        void avancer(Duration d) { t = t.plus(d); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return t; }
    }

    @Autowired CliniqueService clinique;

    private Long creature() {
        Long p = clinique.rechercherProprietaires("fumerolle").get(0).id();
        return clinique.creaturesDuProprietaire(p).get(0).id();
    }

    private Long soigneur() {
        return clinique.soigneursPourEspece("Dragon").get(0).id();
    }

    @Test
    void laProposionNEcritRienPuisLaConfirmationCreeLaConsultation() {
        var service = new PropositionService(clinique, new HorlogeReglable(), Duration.ofMinutes(10));
        Long c = creature();
        int avant = clinique.consultationsDeLaCreature(c).size();

        var p = service.proposer("conv", c, soigneur(), LocalDateTime.of(2031, 2, 1, 10, 0), "Contrôle");
        assertThat(clinique.consultationsDeLaCreature(c)).hasSize(avant);
        assertThat(service.enAttente("conv")).hasSize(1);
        assertThat(service.enAttente("autre")).isEmpty();

        service.confirmer(p.id());
        assertThat(clinique.consultationsDeLaCreature(c)).hasSize(avant + 1);
        assertThat(service.enAttente("conv")).isEmpty();
    }

    @Test
    void uneProposionExpireApresDixMinutes() {
        var horloge = new HorlogeReglable();
        var service = new PropositionService(clinique, horloge, Duration.ofMinutes(10));
        var p = service.proposer("conv", creature(), soigneur(), LocalDateTime.of(2031, 3, 1, 10, 0), "Contrôle");
        horloge.avancer(Duration.ofMinutes(11));
        assertThatThrownBy(() -> service.confirmer(p.id())).isInstanceOf(OperationInvalideException.class)
                .hasMessageContaining("expiré");
    }
}
