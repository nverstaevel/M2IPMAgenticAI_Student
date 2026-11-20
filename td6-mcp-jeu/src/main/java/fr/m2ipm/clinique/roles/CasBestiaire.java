package fr.m2ipm.clinique.roles;

public record CasBestiaire(int id, String creature, String espece, String proprietaire, String symptome,
                           String traitementPresse, Verdict verdictAttendu) {}
