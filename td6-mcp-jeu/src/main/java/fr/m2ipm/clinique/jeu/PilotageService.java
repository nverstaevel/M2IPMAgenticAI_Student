package fr.m2ipm.clinique.jeu;

import fr.m2ipm.clinique.agent.ArretUrgence;
import fr.m2ipm.clinique.scenario.Orchestrateur;
import fr.m2ipm.clinique.scenario.ScenarioDef;
import fr.m2ipm.clinique.scenario.ScenarioLoader;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Lance une partie EN ARRIÈRE-PLAN (une seule à la fois) et garde son état pour la page {@code /pilotage}. Le joueur est un
 * scénario {@code .md} (agents, avec le modèle du profil actif) ou {@link JoueurRegles#ID} (règles fixes, sans modèle).
 */
@Service
public class PilotageService {

    private static final Logger LOG = LoggerFactory.getLogger(PilotageService.class);

    public record Instantane(String statut, String scenario, Long graine, Integer jourEnCours, String runIdEnCours,
                             List<JoueurService.JourJoue> jours, JoueurService.Bilan bilan, String erreur, boolean arretDemande,
                             long pauseMs, long debut, long fin) {}

    private final JoueurService joueur;
    private final JoueurRegles regles;
    private final JeuMcp jeu;
    private final ScenarioLoader chargeur;
    private final Orchestrateur orchestrateur;
    // Un fil par partie : une partie coupée d'urgence peut rester bloquée dans un appel au modèle, elle ne doit pas retenir la suivante.
    private final ExecutorService executeur = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "pilotage-jeu");
        t.setDaemon(true);
        return t;
    });

    private volatile String statut = "AUCUNE";
    private volatile String scenario;
    private volatile Long graine;
    private volatile Integer jourEnCours;
    private volatile String runIdEnCours;
    private volatile JoueurService.Bilan bilan;
    private volatile String erreur;
    private volatile boolean arretDemande;
    private volatile long pauseMs = 1000;
    private volatile long debut;
    private volatile long fin;
    private volatile Future<?> tache;
    private volatile boolean coupee;
    /** Numéro de la partie en cours : une partie plus ancienne (coupée, mais pas encore sortie de son appel au modèle) n'écrit plus rien. */
    private volatile int generation;
    private final List<JoueurService.JourJoue> jours = new CopyOnWriteArrayList<>();

    public PilotageService(JoueurService joueur, JoueurRegles regles, JeuMcp jeu, ScenarioLoader chargeur, Orchestrateur orchestrateur) {
        this.joueur = joueur;
        this.regles = regles;
        this.jeu = jeu;
        this.chargeur = chargeur;
        this.orchestrateur = orchestrateur;
    }

    /** @throws IllegalStateException partie déjà en cours ou jeu non connecté ; IllegalArgumentException scénario inconnu ou invalide */
    public synchronized void demarrer(String idScenario, Long graineDemandee, Long pause) {
        if (pause != null) { regler(pause); }
        if ("EN_COURS".equals(statut) && !coupee) { throw new IllegalStateException("Une partie est déjà en cours : attendez sa fin ou arrêtez-la."); }
        if (!jeu.connecte()) {
            throw new IllegalStateException("Le jeu n'est pas connecté : démarrez jeu-bestiaire (port 8090) puis relancez la clinique avec le profil « jeu ».");
        }
        if (!JoueurRegles.ID.equals(idScenario)) {
            ScenarioDef d = chargeur.charger().stream().filter(s -> s.id().equals(idScenario)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Scénario inconnu : " + idScenario));
            List<String> erreurs = orchestrateur.valider(d);
            if (!erreurs.isEmpty()) { throw new IllegalArgumentException("Scénario invalide : " + String.join(" ; ", erreurs)); }
        }
        scenario = idScenario;
        graine = graineDemandee;
        jours.clear();
        bilan = null;
        erreur = null;
        jourEnCours = null;
        runIdEnCours = null;
        arretDemande = false;
        coupee = false;
        final int gen = ++generation;
        debut = System.currentTimeMillis();
        fin = 0;
        statut = "EN_COURS";
        JoueurService.Ecoute ecoute = new JoueurService.Ecoute() {
            public void jourDemarre(int jour, String runId) { if (gen == generation) { jourEnCours = jour; runIdEnCours = runId; } }
            public void jourTermine(JoueurService.JourJoue j) { if (gen == generation) { jours.add(j); } }
            public boolean arretDemande() { return gen != generation || arretDemande; }
            public long pauseMs() { return pauseMs; }
        };
        tache = executeur.submit(() -> {
            JoueurService.Bilan resultat = null;
            String message = null;
            try {
                resultat = JoueurRegles.ID.equals(idScenario) ? regles.jouer(graineDemandee, ecoute)
                        : joueur.jouer(idScenario, graineDemandee, null, ecoute);
            } catch (RuntimeException e) {
                message = e.getClass().getSimpleName() + " : " + e.getMessage();
                if (gen == generation && !arretDemande) { LOG.warn("Partie « {} » en erreur : {}", idScenario, e.getMessage()); }
            }
            if (gen != generation) { return; }   // une autre partie a été lancée depuis : celle-ci n'écrit plus rien
            if (resultat != null) { bilan = resultat; }
            if (message != null && !arretDemande) { erreur = message; statut = "ERREUR"; }
            else { statut = arretDemande ? "ARRETEE" : "TERMINEE"; }
            fin = System.currentTimeMillis();
        });
    }

    /** L'arrêt prend effet à la fin de la journée en cours : une journée d'agents ne s'interrompt pas en plein milieu. */
    public void arreter() { arretDemande = true; }

    /**
     * Le kill switch : l'arrêt immédiat. Plus aucun outil du jeu n'est exécuté pour la journée en cours, le fil de la partie est
     * interrompu, et la fin de journée n'est PAS forcée. Un appel au modèle déjà parti se termine seul, sans suite.
     */
    public void couper() {
        arretDemande = true;
        coupee = true;
        statut = "ARRETEE";   // tout de suite : on peut relancer une partie sans attendre que l'appel au modèle en vol revienne
        fin = System.currentTimeMillis();
        ArretUrgence.arreter(runIdEnCours);
        Future<?> t = tache;
        if (t != null) { t.cancel(true); }
    }

    /** Pause entre deux actions, réglable en cours de partie (bornée de 0 à 10 s). */
    public void regler(long ms) { pauseMs = Math.max(0, Math.min(10_000, ms)); }

    public Instantane instantane() {
        return new Instantane(statut, scenario, graine, jourEnCours, runIdEnCours, List.copyOf(jours), bilan, erreur, arretDemande, pauseMs, debut, fin);
    }
}
