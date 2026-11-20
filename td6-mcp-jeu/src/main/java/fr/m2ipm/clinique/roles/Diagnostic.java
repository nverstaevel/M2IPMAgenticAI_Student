package fr.m2ipm.clinique.roles;

import java.util.List;

/** Sortie structurée du vétérinaire. */
public record Diagnostic(String creature, String diagnostic, List<String> traitements, String justification) {}
