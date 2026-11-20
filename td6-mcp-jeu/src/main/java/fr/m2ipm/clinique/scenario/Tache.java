package fr.m2ipm.clinique.scenario;

/** Sous-tâche d'un plan : le nom d'un travailleur (clé de « travailleurs » dans le .md) et sa consigne. */
public record Tache(String travailleur, String consigne) {}
