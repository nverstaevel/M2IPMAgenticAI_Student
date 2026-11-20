package fr.m2ipm.clinique.roles;

import java.util.List;

/** Plan structuré produit par le superviseur. */
public record Plan(List<SousTache> sousTaches) {

    public enum Travailleur { DOSSIER, PHARMACIE, AGENDA }

    public record SousTache(Travailleur travailleur, String consigne) {}
}
