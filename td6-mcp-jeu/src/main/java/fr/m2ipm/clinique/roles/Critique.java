package fr.m2ipm.clinique.roles;

import java.util.List;

/** Sortie structurée du gardien : verdict, objections, sources (URL des pages lues). Ce sont SES citations qu'on juge. */
public record Critique(Verdict verdict, List<String> objections, List<String> sources) {}
