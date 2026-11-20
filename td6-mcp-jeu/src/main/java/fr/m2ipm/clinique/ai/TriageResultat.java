package fr.m2ipm.clinique.ai;

import java.util.List;

/** Sortie structurée du triage : le modèle remplit ce record à partir d'un texte libre. */
public record TriageResultat(String espece, Urgence urgence, String resume, List<String> symptomes) {
    public enum Urgence { FAIBLE, MOYENNE, ELEVEE }
}
