package fr.m2ipm.clinique.jeu;

import fr.m2ipm.clinique.scenario.Orchestrateur;
import fr.m2ipm.clinique.scenario.ScenarioDef;
import fr.m2ipm.clinique.scenario.ScenarioLoader;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
                             long debut, long fin) {}

    private final JoueurService joueur;
    private final JoueurRegles regles;
    private final JeuMcp jeu;
    private final ScenarioLoader chargeur;
    private final Orchestrateur orchestrateur;
    private final ExecutorService executeur = Executors.newSingleThreadExecutor(r -> {
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
    private volatile long debut;
    private volatile long fin;
    private final List<JoueurService.JourJoue> jours = new CopyOnWriteArrayList<>();

    public PilotageService(JoueurService joueur, JoueurRegles regles, JeuMcp jeu, ScenarioLoader chargeur, Orchestrateur orchestrateur) {
        this.joueur = joueur;
        this.regles = regles;
        this.jeu = jeu;
        this.chargeur = chargeur;
        this.orchestrateur = orchestrateur;
    }

    /** @throws IllegalStateException partie déjà en cours ou jeu non connecté ; IllegalArgumentException scénario inconnu ou invalide */
    public synchronized void demarrer(String idScenario, Long graineDemandee) {
        if ("EN_COURS".equals(statut)) { throw new IllegalStateException("Une partie est déjà en cours : attendez sa fin ou arrêtez-la."); }
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
        debut = System.currentTimeMillis();
        fin = 0;
        statut = "EN_COURS";
        JoueurService.Ecoute ecoute = new JoueurService.Ecoute() {
            public void jourDemarre(int jour, String runId) { jourEnCours = jour; runIdEnCours = runId; }
            public void jourTermine(JoueurService.JourJoue j) { jours.add(j); }
            public boolean arretDemande() { return arretDemande; }
        };
        executeur.submit(() -> {
            try {
                bilan = JoueurRegles.ID.equals(idScenario) ? regles.jouer(graineDemandee, ecoute)
                        : joueur.jouer(idScenario, graineDemandee, null, ecoute);
                statut = arretDemande ? "ARRETEE" : "TERMINEE";
            } catch (RuntimeException e) {
                LOG.warn("Partie « {} » en erreur : {}", idScenario, e.getMessage());
                erreur = e.getClass().getSimpleName() + " : " + e.getMessage();
                statut = "ERREUR";
            } finally {
                fin = System.currentTimeMillis();
            }
        });
    }

    /** L'arrêt prend effet à la fin de la journée en cours : une journée d'agents ne s'interrompt pas en plein milieu. */
    public void arreter() { arretDemande = true; }

    public Instantane instantane() {
        return new Instantane(statut, scenario, graine, jourEnCours, runIdEnCours, List.copyOf(jours), bilan, erreur, arretDemande, debut, fin);
    }
}
