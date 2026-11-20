package fr.m2ipm.clinique.experience;

/** Une ligne de mesures.csv. Les jetons sont estimés (1 jeton ≈ 4 caractères) : indépendant du modèle utilisé. */
public record Mesure(String question, String strategie, String position, String bruit,
                     int jetonsEntree, int jetonsSortie, long latenceMs, double coutEstime, int exactitude) {

    public static String entete() {
        return "question;strategie;position;bruit;jetons_entree;jetons_sortie;latence_ms;cout_estime;exactitude";
    }

    public String ligne() {
        return String.join(";", question, strategie, position, bruit, String.valueOf(jetonsEntree),
                String.valueOf(jetonsSortie), String.valueOf(latenceMs), String.format(java.util.Locale.ROOT, "%.6f", coutEstime),
                String.valueOf(exactitude));
    }
}
