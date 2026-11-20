package fr.m2ipm.clinique.agent;

/** Pourquoi la boucle s'est arrêtée. Un seul motif est « normal » : REPONSE_FINALE. */
public enum MotifArret {
    REPONSE_FINALE, PLAFOND_ETAPES, BUDGET_TOKENS, BOUCLE_DETECTEE, ERREUR
}
