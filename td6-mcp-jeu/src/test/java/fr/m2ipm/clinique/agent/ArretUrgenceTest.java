package fr.m2ipm.clinique.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ArretUrgenceTest {

    @Test
    void uneExecutionMarqueeEstRefuseeLesAutresNon() {
        ArretUrgence.arreter("r1");
        assertThatThrownBy(() -> ArretUrgence.verifier("r1")).isInstanceOf(ArretUrgence.Arret.class);
        ArretUrgence.verifier("r2");
        ArretUrgence.verifier(null);
        ArretUrgence.oublier("r1");
        ArretUrgence.verifier("r1");
    }

    @Test
    void unAppelAuModeleNEstPasRetenteApresUnArret() {
        AtomicInteger appels = new AtomicInteger();
        ArretUrgence.arreter("r3");
        assertThatThrownBy(() -> SuiviOutils.reessayer(null, "r3", "n", () -> { appels.incrementAndGet(); throw new IllegalStateException("x"); }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(appels.get()).isEqualTo(1);
        ArretUrgence.oublier("r3");
    }
}
