package fr.m2ipm.clinique.roles;

import java.time.Instant;
import java.util.List;

/** Trace complète d'un traitement par le duo : tours, issue, état de validation humaine, éléments techniques. */
public class DossierRun {

    private final String runId;
    private final CasBestiaire cas;
    private final ProfilActeur profil;
    private final List<Tour> tours;
    private final StatutDuo statut;
    private final long dureeMs;
    private final int tokensEstimes;
    private final String modele;
    private final Instant date;
    private volatile Validation validation = Validation.EN_ATTENTE_DE_VALIDATION;
    private volatile String validePar;
    private volatile Instant valideLe;

    public DossierRun(String runId, CasBestiaire cas, ProfilActeur profil, List<Tour> tours, StatutDuo statut,
                      long dureeMs, int tokensEstimes, String modele, Instant date) {
        this.runId = runId;
        this.cas = cas;
        this.profil = profil;
        this.tours = List.copyOf(tours);
        this.statut = statut;
        this.dureeMs = dureeMs;
        this.tokensEstimes = tokensEstimes;
        this.modele = modele;
        this.date = date;
    }

    /** Un dossier ne se valide qu'une fois. */
    public synchronized void decider(boolean accepte, String agent, Instant quand) {
        if (validation != Validation.EN_ATTENTE_DE_VALIDATION) {
            throw new IllegalStateException("Dossier déjà " + validation + " par " + validePar);
        }
        validation = accepte ? Validation.VALIDE : Validation.REFUSE;
        validePar = agent;
        valideLe = quand;
    }

    public String runId() { return runId; }
    public CasBestiaire cas() { return cas; }
    public ProfilActeur profil() { return profil; }
    public List<Tour> tours() { return tours; }
    public StatutDuo statut() { return statut; }
    public long dureeMs() { return dureeMs; }
    public int tokensEstimes() { return tokensEstimes; }
    public String modele() { return modele; }
    public Instant date() { return date; }
    public Validation validation() { return validation; }
    public String validePar() { return validePar; }
    public Instant valideLe() { return valideLe; }

    public Tour dernierTour() { return tours.get(tours.size() - 1); }
}
