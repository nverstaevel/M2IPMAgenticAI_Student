package fr.m2ipm.clinique.externe;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Lit une seule fois, au démarrage, le classeur « tarifs-et-pharmacie.xlsx » (feuilles Tarifs et Pharmacie)
 * et garde les données en mémoire. Le fichier n'est pas relu à chaque appel d'outil.
 */
@Component
public class TarifsExcelReader {

    private final Map<String, Double> tarifs = new HashMap<>();
    private final Map<String, Integer> stocks = new HashMap<>();
    private final Map<String, String> unites = new HashMap<>();

    public TarifsExcelReader(@Value("${clinique.tarifs.fichier:donnees/tarifs-et-pharmacie.xlsx}") String chemin) {
        try (InputStream in = new ClassPathResource(chemin).getInputStream();
             Workbook classeur = new XSSFWorkbook(in)) {
            lireTarifs(classeur.getSheet("Tarifs"));
            lirePharmacie(classeur.getSheet("Pharmacie"));
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire " + chemin, e);
        }
    }

    private void lireTarifs(Sheet feuille) {
        for (Row ligne : feuille) {
            if (ligne.getRowNum() == 0) continue;
            tarifs.put(cle(texte(ligne, 0), texte(ligne, 1)), ligne.getCell(2).getNumericCellValue());
        }
    }

    private void lirePharmacie(Sheet feuille) {
        for (Row ligne : feuille) {
            if (ligne.getRowNum() == 0) continue;
            String nom = normaliser(texte(ligne, 0));
            stocks.put(nom, (int) ligne.getCell(1).getNumericCellValue());
            unites.put(nom, texte(ligne, 2));
        }
    }

    private static String texte(Row ligne, int colonne) {
        return ligne.getCell(colonne).getStringCellValue();
    }

    /** Prix d'un acte pour une espèce, vide si l'acte ou l'espèce est inconnu. */
    public Optional<Double> tarif(String acte, String espece) {
        return Optional.ofNullable(tarifs.get(cle(acte, espece)));
    }

    /** Stock d'un médicament, vide s'il n'est pas dans la pharmacie. */
    public Optional<Integer> stock(String medicament) {
        return Optional.ofNullable(stocks.get(normaliser(medicament)));
    }

    public String unite(String medicament) {
        return unites.getOrDefault(normaliser(medicament), "");
    }

    private static String cle(String acte, String espece) {
        return normaliser(acte) + "|" + normaliser(espece);
    }

    /** Minuscules, sans accents ni espaces superflus : « Élixir » et « elixir » sont le même médicament. */
    static String normaliser(String s) {
        if (s == null) return "";
        String sansAccents = Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sansAccents.toLowerCase(Locale.ROOT);
    }
}
