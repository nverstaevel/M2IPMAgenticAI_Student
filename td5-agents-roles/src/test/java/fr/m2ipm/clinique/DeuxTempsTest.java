package fr.m2ipm.clinique;

import static org.assertj.core.api.Assertions.assertThat;

import fr.m2ipm.clinique.roles.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

/** Mode « deux temps » : analyse libre avec outils, puis conversion en objet par un client sans outils. */
class DeuxTempsTest {

    @Test
    void leVeterinaireAnalyseEnTexteLibrePuisConvertit() {
        var libre = new ModeleTexte("Je pense à une insomnie, tisane de valériane.");
        var convertisseur = new ModeleTexte(DuoServiceTest.DIAG.formatted("tisane"));
        var d = new VeterinaireAgent(ChatClient.create(libre), ChatClient.create(convertisseur))
                .proposer(DuoServiceTest.CAS, ProfilActeur.PRUDENT, java.util.List.of());
        assertThat(d.traitements()).containsExactly("tisane");
        assertThat(libre.dernierUtilisateur()).contains("sans JSON");
        assertThat(convertisseur.dernierUtilisateur()).contains("tisane de valériane");
    }

    @Test
    void leGardienEmetSonAvisEnTexteLibrePuisConvertit() {
        var libre = new ModeleTexte("REJETE : l'argent est nocif. Source : https://fr.wikipedia.org/wiki/Loup-garou");
        var convertisseur = new ModeleTexte(DuoServiceTest.REJETE);
        var c = new GardienDuBestiaire(ChatClient.create(libre), ChatClient.create(convertisseur))
                .examiner("Loup-garou", new Diagnostic("Lunatique", "Insomnie", java.util.List.of("argent"), "j"));
        assertThat(c.verdict()).isEqualTo(Verdict.REJETE);
        assertThat(libre.dernierUtilisateur()).contains("sans JSON");
        assertThat(convertisseur.dernierUtilisateur()).contains("l'argent est nocif");
    }
}
