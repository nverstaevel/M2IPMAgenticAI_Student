package fr.m2ipm.clinique.experience;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.*;
import java.util.function.Function;
import org.springframework.core.io.ClassPathResource;

/**
 * Expérience « contexte » : comment donner au modèle la bonne information dans un grand corpus ?
 * <ul>
 *   <li>Stratégie A : tout le texte dans le prompt (tronqué à {@code tailleMax} caractères) ;</li>
 *   <li>Stratégie B : un outil de recherche renvoie au plus 3 passages de 800 caractères ;</li>
 *   <li>variations : position du passage utile (début, milieu, fin) et bruit (aucun, moitié, double).</li>
 * </ul>
 * Le modèle est un simple {@code Function<String,String>} (prompt → réponse) : un vrai ChatClient en production,
 * un faux dans les tests. Les coûts sont fictifs (voir {@link #TARIF_ENTREE}).
 */
public class ExperienceContexte {

    public static final double TARIF_ENTREE = 0.000002;   // € par jeton d'entrée (fictif)
    public static final double TARIF_SORTIE = 0.000006;   // € par jeton de sortie (fictif)
    static final int PASSAGES_MAX = 3;
    static final int PASSAGE_LONGUEUR = 800;

    public record Question(String id, String texte, String attendu) {}

    private final List<String> paragraphes;
    private final List<Question> questions;
    private final Function<String, String> modele;

    public ExperienceContexte(Function<String, String> modele) {
        this(lireRessource("experience/grand-bestiaire.txt"), lireRessource("experience/questions-reference.csv"), modele);
    }

    public ExperienceContexte(String corpus, String questionsCsv, Function<String, String> modele) {
        this.paragraphes = Arrays.stream(corpus.split("\n\\s*\n")).map(String::strip).filter(s -> !s.isEmpty()).toList();
        this.questions = Arrays.stream(questionsCsv.split("\n")).skip(1).filter(l -> !l.isBlank())
                .map(l -> l.split(";", 3)).map(c -> new Question(c[0], c[1], c[2].strip())).toList();
        this.modele = modele;
    }

    public List<Question> questions() {
        return questions;
    }

    /** Stratégie A : le corpus complet, tronqué, est collé dans le prompt. */
    public Mesure strategieA(Question q, int tailleMax) {
        String contexte = String.join("\n\n", paragraphes);
        if (contexte.length() > tailleMax) contexte = contexte.substring(0, tailleMax);
        return mesurer(q, "A", "-", "-", prompt(contexte, q));
    }

    /** Stratégie B : seuls les passages retrouvés par {@link #chercherDansBestiaire} sont fournis. */
    public Mesure strategieB(Question q) {
        return mesurer(q, "B", "-", "-", prompt(String.join("\n\n", chercherDansBestiaire(q.texte())), q));
    }

    /**
     * Variation : le passage utile est placé au début, au milieu ou à la fin d'un contexte de remplissage
     * dont la taille dépend du bruit (« aucun » = 10 paragraphes, « moitie » = 15, « double » = 30 autour du passage).
     */
    public Mesure avecVariation(Question q, String position, String bruit) {
        String utile = paragraphes.stream().filter(p -> contient(p, q.attendu())).findFirst().orElseThrow();
        List<String> remplissage = paragraphes.stream().filter(p -> !p.equals(utile)).toList();
        int n = switch (bruit) { case "aucun" -> 10; case "moitie" -> 15; case "double" -> 30; default -> throw new IllegalArgumentException(bruit); };
        List<String> contexte = new ArrayList<>(remplissage.subList(0, Math.min(n, remplissage.size())));
        int index = switch (position) { case "debut" -> 0; case "milieu" -> contexte.size() / 2; case "fin" -> contexte.size(); default -> throw new IllegalArgumentException(position); };
        contexte.add(index, utile);
        return mesurer(q, "A", position, bruit, prompt(String.join("\n\n", contexte), q));
    }

    /** Recherche par mots-clés : au plus 3 paragraphes, 800 caractères chacun, triés par nombre de mots communs. */
    public List<String> chercherDansBestiaire(String motsCles) {
        Set<String> mots = mots(motsCles);
        return paragraphes.stream()
                .map(p -> Map.entry(p, mots(p).stream().filter(mots::contains).count()))
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(PASSAGES_MAX)
                .map(e -> e.getKey().length() > PASSAGE_LONGUEUR ? e.getKey().substring(0, PASSAGE_LONGUEUR) : e.getKey())
                .toList();
    }

    /** Exécute les 10 questions avec A (contexte tronqué) et B, et écrit le CSV. */
    public List<Mesure> lancer(int tailleMaxA, Path sortie) {
        List<Mesure> mesures = new ArrayList<>();
        for (Question q : questions) {
            mesures.add(strategieA(q, tailleMaxA));
            mesures.add(strategieB(q));
        }
        ecrire(mesures, sortie);
        return mesures;
    }

    public static void ecrire(List<Mesure> mesures, Path sortie) {
        List<String> lignes = new ArrayList<>();
        lignes.add(Mesure.entete());
        mesures.forEach(m -> lignes.add(m.ligne()));
        try {
            if (sortie.getParent() != null) Files.createDirectories(sortie.getParent());
            Files.write(sortie, lignes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Fonction de mesure commune : chronomètre, jetons estimés, coût fictif, exactitude 0/1. */
    Mesure mesurer(Question q, String strategie, String position, String bruit, String prompt) {
        long debut = System.nanoTime();
        String reponse = modele.apply(prompt);
        long ms = (System.nanoTime() - debut) / 1_000_000;
        int in = jetons(prompt);
        int out = jetons(reponse);
        double cout = in * TARIF_ENTREE + out * TARIF_SORTIE;
        return new Mesure(q.id(), strategie, position, bruit, in, out, ms, cout, contient(reponse, q.attendu()) ? 1 : 0);
    }

    private static String prompt(String contexte, Question q) {
        return "Réponds uniquement d'après le contexte.\n\nContexte :\n" + contexte + "\n\nQuestion : " + q.texte();
    }

    static int jetons(String s) {
        return (s == null ? 0 : s.length() + 3) / 4;
    }

    static boolean contient(String texte, String attendu) {
        return normaliser(texte).contains(normaliser(attendu));
    }

    private static Set<String> mots(String s) {
        Set<String> r = new HashSet<>();
        for (String m : normaliser(s).split("[^a-z0-9]+")) {
            if (m.length() > 3) r.add(m);
        }
        return r;
    }

    private static String normaliser(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    private static String lireRessource(String chemin) {
        try {
            return new String(new ClassPathResource(chemin).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
