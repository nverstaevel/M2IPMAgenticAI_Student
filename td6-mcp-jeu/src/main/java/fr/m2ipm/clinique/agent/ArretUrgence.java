package fr.m2ipm.clinique.agent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Le coupe-circuit : l'arrêt d'urgence d'une exécution (page /pilotage, bouton « Couper tout »). Une exécution marquée est
 * refusée à chaque appel d'outil et à chaque nouveau nœud du graphe : l'agent ne peut plus rien faire dans le jeu, même s'il
 * est au milieu d'une réflexion. Un appel au modèle déjà parti ne peut pas être rappelé ; il se termine, et rien ne suit.
 */
public final class ArretUrgence {

    /** Levée à la place de l'action refusée. */
    public static final class Arret extends RuntimeException {
        public Arret(String runId) { super("Arrêt d'urgence demandé (exécution " + runId + ")."); }
    }

    private static final Set<String> RUNS = ConcurrentHashMap.newKeySet();

    private ArretUrgence() {}

    public static void arreter(String runId) { if (runId != null) { RUNS.add(runId); } }

    public static boolean demande(String runId) { return runId != null && RUNS.contains(runId); }

    /** @throws Arret si l'arrêt d'urgence a été demandé pour cette exécution */
    public static void verifier(String runId) { if (demande(runId)) { throw new Arret(runId); } }

    /** À appeler quand l'exécution est finie : plus rien ne s'y rapporte. */
    public static void oublier(String runId) { if (runId != null) { RUNS.remove(runId); } }
}
