package fr.m2ipm.jeu;

import java.util.List;
import java.util.Map;

/** Règles fixes du jeu : espèces, maladies, médicaments. Seul le tirage des patients dépend de la graine. */
final class Catalogue {

    private Catalogue() {}

    /** Une maladie : les symptômes visibles sont communs aux deux maladies d'une espèce, le signe distinctif exige un examen. */
    record Maladie(String id, String espece, String nom, String signeDistinctif, String remede) {}

    record Medicament(String nom, int prix, String description, int stockInitial) {}

    static final List<String> ESPECES = List.of("dragonnet", "loup-garou", "vampire", "licorne");

    /** Symptômes visibles à la consultation (ils ne suffisent pas à distinguer les deux maladies de l'espèce). */
    static final Map<String, List<String>> SYMPTOMES = Map.of(
            "dragonnet", List.of("abattement", "écailles ternes"),
            "loup-garou", List.of("agitation", "poil terne"),
            "vampire", List.of("pâleur", "fatigue"),
            "licorne", List.of("tristesse", "crinière emmêlée"));

    static final List<Maladie> MALADIES = List.of(
            new Maladie("fievre-de-braise", "dragonnet", "Fièvre de braise", "écailles brûlantes au toucher", "eau-de-glacier"),
            new Maladie("toux-de-soufre", "dragonnet", "Toux de soufre", "fumée jaune à chaque toux", "sirop-de-miel"),
            new Maladie("lycanthropie-aigue", "loup-garou", "Lycanthropie aiguë", "transformation incontrôlée en pleine journée", "aconit-dilue"),
            new Maladie("puces-lunaires", "loup-garou", "Puces lunaires", "démangeaisons qui empirent à la nuit tombée", "baume-de-lune"),
            new Maladie("anemie-nocturne", "vampire", "Anémie nocturne", "gencives grises, canines émoussées", "sang-de-synthese"),
            new Maladie("photophobie", "vampire", "Photophobie", "paupières brûlées par la lumière du jour", "creme-d-ombre"),
            new Maladie("corne-ternie", "licorne", "Corne ternie", "corne sans aucun éclat", "poudre-d-aube"),
            new Maladie("sabots-fendus", "licorne", "Sabots fendus", "sabots qui se fendent sur la pierre", "onguent-de-nacre"));

    static final List<Medicament> MEDICAMENTS = List.of(
            new Medicament("eau-de-glacier", 20, "Rafraîchit les écailles brûlantes", 2),
            new Medicament("sirop-de-miel", 12, "Apaise les gorges irritées", 2),
            new Medicament("aconit-dilue", 35, "Calme la transformation (dilué : sans danger)", 2),
            new Medicament("baume-de-lune", 18, "Soulage les démangeaisons nocturnes", 2),
            new Medicament("sang-de-synthese", 40, "Reconstitue le sang des créatures nocturnes", 2),
            new Medicament("creme-d-ombre", 25, "Protège des rayons du soleil", 2),
            new Medicament("poudre-d-aube", 50, "Redonne son éclat à la corne", 2),
            new Medicament("onguent-de-nacre", 30, "Répare les sabots et les griffes", 2),
            new Medicament("argent-colloidal", 30, "Désinfectant puissant", 3),
            new Medicament("gousse-d-ail", 5, "Assainissant, très bon marché", 3),
            new Medicament("tisane-de-sauge", 8, "Ne soigne pas, mais redonne des forces (+15 PV)", 5));

    /** Médicaments toxiques par espèce (le gardien du bestiaire les connaît, la pharmacie ne vous avertit pas). */
    static final Map<String, List<String>> CONTRE_INDICATIONS = Map.of(
            "loup-garou", List.of("argent-colloidal"),
            "vampire", List.of("gousse-d-ail"),
            "dragonnet", List.of(),
            "licorne", List.of());

    static Medicament medicament(String nom) {
        return MEDICAMENTS.stream().filter(m -> m.nom().equalsIgnoreCase(nom)).findFirst().orElse(null);
    }

    static List<Maladie> maladiesDe(String espece) {
        return MALADIES.stream().filter(m -> m.espece().equals(espece)).toList();
    }
}
