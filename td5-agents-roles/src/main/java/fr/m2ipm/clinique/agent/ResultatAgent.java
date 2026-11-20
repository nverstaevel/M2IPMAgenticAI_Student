package fr.m2ipm.clinique.agent;

import java.util.List;

public record ResultatAgent(String reponse, MotifArret motif, List<Etape> etapes, int tokens) {}
