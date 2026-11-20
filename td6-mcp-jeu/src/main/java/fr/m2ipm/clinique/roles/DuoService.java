package fr.m2ipm.clinique.roles;

import fr.m2ipm.clinique.agent.Evenement;
import fr.m2ipm.clinique.agent.SuiviExecution;
import fr.m2ipm.clinique.agent.SuiviOutils;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Boucle actor-critic : le vétérinaire propose, le gardien critique, trois tours au plus. */
@Service
public class DuoService {

    public static final int MAX_TOURS = 3;

    private final VeterinaireAgent veterinaire;
    private final GardienDuBestiaire gardien;
    private final Clock horloge;
    private final String modele;
    private final SuiviExecution suivi;
    private final Map<String, DossierRun> runs = new ConcurrentHashMap<>();

    @Autowired
    public DuoService(VeterinaireAgent veterinaire, GardienDuBestiaire gardien, SuiviExecution suivi,
                      @Value("${spring.ai.model.chat:inconnu}") String modele) {
        this(veterinaire, gardien, Clock.systemDefaultZone(), modele, suivi);
    }

    public DuoService(VeterinaireAgent veterinaire, GardienDuBestiaire gardien, Clock horloge, String modele) {
        this(veterinaire, gardien, horloge, modele, new SuiviExecution());
    }

    public DuoService(VeterinaireAgent veterinaire, GardienDuBestiaire gardien, Clock horloge, String modele, SuiviExecution suivi) {
        this.veterinaire = veterinaire;
        this.gardien = gardien;
        this.horloge = horloge;
        this.modele = modele;
        this.suivi = suivi;
    }

    public DossierRun traiter(CasBestiaire cas, ProfilActeur profil) {
        return traiter(UUID.randomUUID().toString(), cas, profil);
    }

    /** Le runId est choisi par l'appelant pour que la page /agents puisse suivre l'exécution avant sa fin. */
    public DossierRun traiter(String runId, CasBestiaire cas, ProfilActeur profil) {
        suivi.demarrer(runId, "Duo : cas " + cas.id() + " · " + cas.creature() + " (" + profil + ")");
        suivi.noeud(runId, "cas", List.of(), Evenement.UTILISATEUR, "Cas " + cas.id() + " · " + cas.creature(), Evenement.OK,
                cas.espece() + " — symptôme : " + cas.symptome() + "\nProfil du vétérinaire : " + profil, null, null, null);
        long debut = System.nanoTime();
        List<Tour> tours = new ArrayList<>();
        List<String> objections = List.of();
        int caracteres = 0;
        StatutDuo statut = StatutDuo.NON_CONVERGENTE;
        String precedent = "cas";
        String enCours = null;
        try {
            for (int numero = 1; numero <= MAX_TOURS; numero++) {
                String idV = "v" + numero, idG = "g" + numero;
                enCours = idV;
                suivi.noeud(runId, idV, List.of(precedent), Evenement.AGENT, "Vétérinaire · tour " + numero, Evenement.EN_COURS,
                        objections.isEmpty() ? null : "Objections du gardien à prendre en compte :\n- " + String.join("\n- ", objections),
                        null, null, null);
                long t0 = System.nanoTime();
                Diagnostic d;
                try (var p = SuiviOutils.ouvrir(suivi, runId, idV)) {
                    var objectionsTour = objections;
                    d = SuiviOutils.reessayer(suivi, runId, idV, () -> veterinaire.proposer(cas, profil, objectionsTour));
                }
                suivi.maj(runId, idV, Evenement.OK, resume(d), null, ms(t0), null);

                enCours = idG;
                suivi.noeud(runId, idG, List.of(idV), Evenement.CRITIQUE, "Gardien · tour " + numero, Evenement.EN_COURS,
                        "Examine : " + d.diagnostic() + " — " + String.join(" ; ", d.traitements()), null, null, null);
                long t1 = System.nanoTime();
                Critique c;
                try (var p = SuiviOutils.ouvrir(suivi, runId, idG)) {
                    var dTour = d;
                    c = SuiviOutils.reessayer(suivi, runId, idG, () -> gardien.examiner(cas.espece(), dTour));
                }
                suivi.maj(runId, idG, c.verdict() == Verdict.APPROUVE ? Evenement.OK : Evenement.ERREUR,
                        "Verdict : " + c.verdict(), resume(c), ms(t1), null);
                precedent = idG;

                tours.add(new Tour(numero, d, c));
                caracteres += d.toString().length() + c.toString().length();
                if (c.verdict() == Verdict.APPROUVE) { statut = StatutDuo.APPROUVEE; break; }
                if (c.verdict() == Verdict.DOUTE) { statut = StatutDuo.VERS_HUMAIN; break; }
                objections = c.objections() == null ? List.of() : c.objections();
            }
        } catch (RuntimeException e) {
            if (enCours != null) {
                suivi.maj(runId, enCours, Evenement.ERREUR, null, String.valueOf(e.getMessage()), null, null);
            }
            suivi.noeud(runId, "fin", List.of(enCours == null ? "cas" : enCours), Evenement.FIN, "Erreur", Evenement.ERREUR,
                    String.valueOf(e.getMessage()), null, null, null);
            suivi.terminer(runId, "ERREUR");
            throw e;
        }
        long ms = (System.nanoTime() - debut) / 1_000_000;
        var run = new DossierRun(runId, cas, profil, tours, statut, ms, caracteres / 4, modele, Instant.now(horloge));
        runs.put(run.runId(), run);

        suivi.noeud(runId, "humain", List.of(precedent), Evenement.HUMAIN, "Validation humaine (PERSONNEL)", Evenement.ATTENTE,
                "Aucun traitement n'est appliqué sans la décision d'un membre du personnel : POST /api/dossiers/" + runId + "/valider",
                null, null, null);
        suivi.noeud(runId, "fin", List.of("humain"), Evenement.FIN, "Dossier : " + statut,
                statut == StatutDuo.APPROUVEE ? Evenement.OK : Evenement.ERREUR,
                tours.size() + " tour(s), vérification du gardien : " + tours.get(tours.size() - 1).critique().verdict(),
                null, ms, run.tokensEstimes());
        suivi.terminer(runId, statut.name());
        return run;
    }

    private static String resume(Diagnostic d) {
        return "Diagnostic : " + d.diagnostic() + "\nTraitements : " + String.join(" ; ", d.traitements()) + "\nJustification : " + d.justification();
    }

    private static String resume(Critique c) {
        return "Verdict : " + c.verdict()
                + (c.objections() == null || c.objections().isEmpty() ? "" : "\nObjections :\n- " + String.join("\n- ", c.objections()))
                + (c.sources() == null || c.sources().isEmpty() ? "" : "\nSources : " + String.join(" ; ", c.sources()));
    }

    private static long ms(long debutNanos) { return (System.nanoTime() - debutNanos) / 1_000_000; }

    public Optional<DossierRun> run(String runId) { return Optional.ofNullable(runs.get(runId)); }

    public Collection<DossierRun> runs() { return runs.values(); }
}
