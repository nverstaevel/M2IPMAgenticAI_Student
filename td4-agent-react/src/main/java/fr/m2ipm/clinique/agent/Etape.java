package fr.m2ipm.clinique.agent;

/**
 * Une étape = un appel au modèle suivi de l'exécution des outils demandés.
 * {@code raisonnement} est le texte que le modèle a écrit avant l'outil (ou sa réponse finale) :
 * ce n'est PAS une explication garantie de ce qu'il a calculé. {@code outil} et {@code arguments} sont vides
 * pour la dernière étape ; s'il y a plusieurs outils dans la même étape, les noms sont joints par « + »
 * et les arguments forment un tableau JSON.
 */
public record Etape(int numero, String raisonnement, String outil, String arguments, String resultat,
                    long dureeMs, int tokens) {}
