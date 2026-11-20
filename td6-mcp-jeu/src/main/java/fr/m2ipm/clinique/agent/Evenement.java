package fr.m2ipm.clinique.agent;

import java.util.List;

/**
 * Un événement du suivi en direct d'une exécution. Le graphe affiché dans {@code /agents} est reconstruit côté navigateur :
 * un événement {@code noeud} crée ou met à jour le nœud {@code id} (les champs {@code null} restent inchangés).
 * {@code type} : {@code demarrage} (libelle = titre du run), {@code noeud}, {@code fin} (statut = motif d'arrêt).
 */
public record Evenement(long seq, long horodatage, String runId, String type, String id, List<String> parents,
                        String genre, String libelle, String statut, String detail, String resultat,
                        Long dureeMs, Integer tokens, Integer tokensEntree, Integer tokensSortie,
                        Integer appelsModele, Boolean tokensEstimes) {

    /** Genres de nœuds (couleur et forme dans le graphe). */
    public static final String UTILISATEUR = "UTILISATEUR";
    public static final String MODELE = "MODELE";
    public static final String OUTIL = "OUTIL";
    public static final String AGENT = "AGENT";
    public static final String CRITIQUE = "CRITIQUE";
    public static final String TRAVAILLEUR = "TRAVAILLEUR";
    public static final String HUMAIN = "HUMAIN";
    public static final String CODE = "CODE";
    public static final String FIN = "FIN";

    /** Statuts d'un nœud. */
    public static final String EN_COURS = "EN_COURS";
    public static final String OK = "OK";
    public static final String ERREUR = "ERREUR";
    public static final String ATTENTE = "ATTENTE";
    public static final String PREVU = "PREVU";
}
