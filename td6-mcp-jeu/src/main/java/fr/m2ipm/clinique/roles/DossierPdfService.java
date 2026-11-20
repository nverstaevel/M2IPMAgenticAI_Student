package fr.m2ipm.clinique.roles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Dossier PDF d'un traitement : en-tête, un bloc par tour, décision et validation humaine, annexe technique.
 * Ce n'est PAS une trace d'audit complète : ni prompts, ni version exacte du modèle, ni texte intégral des pages lues.
 */
@Service
public class DossierPdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.FRANCE)
            .withZone(ZoneId.of("Europe/Paris"));
    private static final float MARGE = 50, LARGEUR = PDRectangle.A4.getWidth() - 2 * MARGE;

    private final DuoService duo;
    private final Path dossier;

    @Autowired
    public DossierPdfService(DuoService duo, @Value("${dossiers.dir:dossiers}") String dossier) {
        this(duo, Path.of(dossier));
    }

    public DossierPdfService(DuoService duo, Path dossier) {
        this.duo = duo;
        this.dossier = dossier;
    }

    /** Génère le PDF du dossier, l'écrit dans dossiers/{runId}.pdf et renvoie ses octets. */
    public byte[] generer(String runId) {
        DossierRun run = duo.run(runId).orElseThrow(() -> new IllegalArgumentException("Dossier inconnu : " + runId));
        byte[] pdf = rendre(run);
        try {
            Files.createDirectories(dossier);
            Files.write(dossier.resolve(runId + ".pdf"), pdf);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return pdf;
    }

    byte[] rendre(DossierRun run) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Page p = new Page(doc);
            CasBestiaire cas = run.cas();
            p.titre("Clinique des créatures fantastiques — Dossier de traitement");
            p.ligne("Date : " + DATE.format(run.date()) + "    Dossier : " + run.runId());
            p.ligne("Patient : " + cas.creature() + " (" + cas.espece() + "), propriétaire : " + cas.proprietaire() + " (fictif)");
            p.ligne("Motif : " + cas.symptome());
            p.espace();
            for (Tour t : run.tours()) {
                p.sousTitre("Tour " + t.numero());
                p.ligne("Vétérinaire — diagnostic : " + t.diagnostic().diagnostic());
                p.ligne("Traitements proposés : " + String.join(" ; ", sure(t.diagnostic().traitements())));
                p.ligne("Justification : " + t.diagnostic().justification());
                p.ligne("Gardien — verdict : " + t.critique().verdict());
                for (String o : sure(t.critique().objections())) p.ligne("  Objection : " + o);
                for (String s : sure(t.critique().sources())) p.ligne("  Source : " + s);
                p.espace();
            }
            p.sousTitre("Décision");
            p.ligne("Issue du duo : " + run.statut());
            p.ligne("État : " + run.validation()
                    + (run.validePar() == null ? "" : " par " + run.validePar() + " le " + DATE.format(run.valideLe())));
            p.ligne("Aucun traitement n'est appliqué tant que ce dossier n'est pas validé par un agent humain.");
            p.espace();
            p.sousTitre("Annexe technique");
            p.ligne("Durée : " + run.dureeMs() + " ms ; tokens (estimation des sorties) : " + run.tokensEstimes());
            p.ligne("Modèle (réglage spring.ai.model.chat) : " + run.modele() + " ; profil de l'acteur : " + run.profil());
            p.fermer();
            doc.save(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> sure(List<String> l) { return l == null ? List.of() : l; }

    /** Mise en page minimale : texte à la suite, retour à la ligne, saut de page. */
    private static final class Page {
        private final PDDocument doc;
        private final PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDType1Font gras = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private PDPageContentStream flux;
        private float y;

        Page(PDDocument doc) throws IOException {
            this.doc = doc;
            nouvellePage();
        }

        private void nouvellePage() throws IOException {
            if (flux != null) flux.close();
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            flux = new PDPageContentStream(doc, page);
            y = PDRectangle.A4.getHeight() - MARGE;
        }

        void titre(String s) throws IOException { ecrire(s, gras, 14, 22); }
        void sousTitre(String s) throws IOException { ecrire(s, gras, 12, 18); }
        void ligne(String s) throws IOException { ecrire(s, normal, 10, 14); }
        void espace() throws IOException { y -= 8; }

        void fermer() throws IOException { flux.close(); }

        private void ecrire(String texte, PDType1Font police, float taille, float interligne) throws IOException {
            for (String l : decouper(nettoyer(texte), police, taille)) {
                if (y < MARGE + interligne) nouvellePage();
                flux.beginText();
                flux.setFont(police, taille);
                flux.newLineAtOffset(MARGE, y);
                flux.showText(l);
                flux.endText();
                y -= interligne;
            }
        }

        private static List<String> decouper(String texte, PDType1Font police, float taille) throws IOException {
            List<String> lignes = new ArrayList<>();
            StringBuilder courante = new StringBuilder();
            for (String mot : texte.split(" ")) {
                String essai = courante.isEmpty() ? mot : courante + " " + mot;
                if (police.getStringWidth(essai) / 1000 * taille > LARGEUR && !courante.isEmpty()) {
                    lignes.add(courante.toString());
                    courante = new StringBuilder(mot);
                } else {
                    courante = new StringBuilder(essai);
                }
            }
            lignes.add(courante.toString());
            return lignes;
        }

        /** Les polices standard n'encodent que WinAnsi : on remplace le reste par « ? ». */
        static String nettoyer(String s) {
            StringBuilder sb = new StringBuilder();
            for (char c : (s == null ? "" : s).toCharArray()) {
                if (c == '\n' || c == '\t') sb.append(' ');
                else if (c < 32) continue;
                else if (c <= 255 || "—–…€’‘“”œŒ".indexOf(c) >= 0) sb.append(c);
                else sb.append('?');
            }
            return sb.toString();
        }
    }
}
