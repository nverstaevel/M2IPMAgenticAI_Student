package fr.m2ipm.clinique.scenario;

import java.util.List;

/**
 * Ce qu'un agent rend à l'orchestrateur. Seul {@code texte} est obligatoire ; le reste dépend du rôle de l'agent dans le graphe :
 * un critique renvoie une {@code decision} et des {@code remarques}, un planificateur renvoie des {@code taches},
 * un routeur renvoie une {@code etiquette} de son choix, et {@code valeur} peut porter n'importe quel objet à transmettre
 * à l'agent suivant. Les arcs étiquetés du graphe (« a -> b : REJETE ») se déclenchent d'après l'étiquette ou la décision.
 */
public record Resultat(String texte, Object valeur, Decision decision, List<String> remarques, List<Tache> taches, String etiquette) {

    public Resultat {
        texte = texte == null ? "" : texte;
        remarques = remarques == null ? List.of() : List.copyOf(remarques);
        taches = taches == null ? List.of() : List.copyOf(taches);
    }

    public Resultat(String texte, Object valeur, Decision decision, List<String> remarques, List<Tache> taches) {
        this(texte, valeur, decision, remarques, taches, null);
    }

    public static Resultat texte(String texte) {
        return new Resultat(texte, null, null, List.of(), List.of(), null);
    }

    public static Resultat de(String texte, Object valeur) {
        return new Resultat(texte, valeur, null, List.of(), List.of(), null);
    }

    public static Resultat avis(Decision decision, List<String> remarques) {
        String t = "Verdict : " + decision + (remarques == null || remarques.isEmpty() ? "" : "\nRemarques :\n- " + String.join("\n- ", remarques));
        return new Resultat(t, null, decision, remarques, List.of(), null);
    }

    public static Resultat plan(List<Tache> taches) {
        String t = taches == null ? "" : taches.stream().map(x -> x.travailleur() + " : " + x.consigne())
                .reduce((a, b) -> a + "\n" + b).orElse("(plan vide)");
        return new Resultat(t, null, null, List.of(), taches, null);
    }

    /** Un routeur : choisit lui-même l'arc à suivre, par une étiquette (en majuscules dans le graphe). */
    public static Resultat route(String texte, String etiquette) {
        return new Resultat(texte, null, null, List.of(), List.of(), etiquette);
    }

    public Resultat avecValeur(Object valeur) {
        return new Resultat(texte, valeur, decision, remarques, taches, etiquette);
    }

    public Resultat avecEtiquette(String etiquette) {
        return new Resultat(texte, valeur, decision, remarques, taches, etiquette);
    }
}
