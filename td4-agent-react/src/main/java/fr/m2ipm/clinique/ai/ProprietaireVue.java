package fr.m2ipm.clinique.ai;

/** Ce que le modèle voit d'un propriétaire : pas de téléphone (donnée personnelle inutile à la conversation). */
public record ProprietaireVue(Long id, String nom, String prenom, String ville) {}
