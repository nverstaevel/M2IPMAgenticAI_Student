package fr.m2ipm.clinique.scenario;

import java.util.List;

/** Un champ de saisie du scénario, affiché dans la page /agents. type : texte, nombre ou choix. */
public record ChampEntree(String id, String libelle, String type, String defaut, List<Option> options) {

    public record Option(String valeur, String libelle) {}
}
