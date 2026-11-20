package fr.m2ipm.clinique.scenario;

/**
 * Permet à un agent d'ajouter ses propres nœuds au graphe, sous son nœud (par exemple une boucle ReAct écrite à la main :
 * un nœud par étape du modèle, un par outil). Les appels d'outils faits via {@code SuiviOutils.envelopper} et les clients
 * ChatClient munis de {@code ComptageJetons.advisor()} apparaissent déjà tout seuls.
 */
public interface Trace {

    /** Crée un nœud EN_COURS sous le nœud de l'agent et renvoie son identifiant. genre : voir Evenement (MODELE, OUTIL, CODE…). */
    String noeud(String genre, String libelle, String detail);

    /** Clôt un nœud créé par {@link #noeud} : ok = vert, sinon rouge ; le résultat s'affiche dans le panneau de détail. */
    void terminer(String id, boolean ok, String resultat);
}
