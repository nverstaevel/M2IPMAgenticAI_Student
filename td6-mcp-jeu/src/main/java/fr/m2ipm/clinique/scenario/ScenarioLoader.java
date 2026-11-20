package fr.m2ipm.clinique.scenario;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Lit les scénarios : les fichiers .md du dossier {@code scenarios.dir} (par défaut ./scenarios, relu à chaque appel :
 * on modifie un fichier, on actualise la page) puis ceux fournis dans les ressources (classpath:scenarios/).
 * Un fichier du dossier remplace celui des ressources qui a le même id (le nom du fichier, sans « .md »). Les fichiers dont le nom commence par « _ » sont ignorés.
 */
@Component
public class ScenarioLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ScenarioLoader.class);

    private final Path dossier;

    public ScenarioLoader(@Value("${scenarios.dir:scenarios}") String dossier) {
        this.dossier = Path.of(dossier);
    }

    public List<ScenarioDef> charger() {
        Map<String, ScenarioDef> parId = new LinkedHashMap<>();
        try {
            for (Resource r : new PathMatchingResourcePatternResolver().getResources("classpath*:scenarios/*.md")) {
                String nom = r.getFilename();
                if (nom == null || nom.startsWith("_")) { continue; }
                try {
                    ajouter(parId, lire(new String(r.getInputStream().readAllBytes(), StandardCharsets.UTF_8), "(fourni) " + nom, nom));
                } catch (IOException e) {
                    LOG.warn("Scénario {} illisible : {}", nom, e.getMessage());
                }
            }
        } catch (IOException e) {
            LOG.debug("Pas de scénario dans les ressources : {}", e.getMessage());
        }
        if (Files.isDirectory(dossier)) {
            try (Stream<Path> fichiers = Files.list(dossier)) {
                for (Path f : fichiers.filter(p -> p.getFileName().toString().endsWith(".md")).sorted().toList()) {
                    String nom = f.getFileName().toString();
                    if (nom.startsWith("_")) { continue; }
                    try {
                        ajouter(parId, lire(Files.readString(f, StandardCharsets.UTF_8), dossier + "/" + nom, nom));
                    } catch (IOException e) {
                        LOG.warn("Scénario {} illisible : {}", f, e.getMessage());
                    }
                }
            } catch (IOException e) {
                LOG.warn("Dossier de scénarios illisible : {}", e.getMessage());
            }
        }
        return new ArrayList<>(parId.values());
    }

    private static void ajouter(Map<String, ScenarioDef> parId, ScenarioDef d) {
        parId.put(d.id(), d);
    }

    private static final Pattern CHAMP = Pattern.compile("^([A-Za-z][A-Za-z0-9_]*)\\s*(?:\\(([^)]*)\\))?\\s*:\\s*(.*)$");
    private static final Pattern MAX = Pattern.compile("\\(\\s*max\\s+(\\d+)\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FLECHE = Pattern.compile("->|=>");

    /** Normalise un nom : minuscules, sans accent, tirets à la place des espaces et de la ponctuation. */
    static String norm(String s) {
        String t = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return t.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }

    /**
     * Analyse un fichier : un titre « # … », des sections « ## Entrées », « ## Agents » et « ## Graphe », et du texte libre
     * (la description). Ne lève jamais : les problèmes deviennent des erreurs du scénario, avec le numéro de ligne.
     */
    static ScenarioDef lire(String contenu, String fichier, String nomFichier) {
        String id = norm(nomFichier.replaceAll("\\.md$", ""));
        List<String> erreurs = new ArrayList<>();
        String titre = null;
        StringBuilder description = new StringBuilder();
        List<ChampEntree> entree = new ArrayList<>();
        List<ScenarioDef.Noeud> agents = new ArrayList<>();
        List<ScenarioDef.Arc> arcs = new ArrayList<>();
        String section = "";
        String[] lignes = contenu.replace("\r\n", "\n").split("\n", -1);
        for (int i = 0; i < lignes.length; i++) {
            String brut = lignes[i];
            String l = brut.strip();
            int n = i + 1;
            if (l.startsWith("## ")) {
                section = switch (norm(l.substring(3))) {
                    case "entrees" -> "entrees";
                    case "agents" -> "agents";
                    case "graphe" -> "graphe";
                    default -> "";
                };
                if (section.isEmpty()) { description.append(brut).append('\n'); }
                continue;
            }
            if (l.startsWith("# ") && titre == null) { titre = l.substring(2).strip(); continue; }
            if (section.isEmpty()) { description.append(brut).append('\n'); continue; }
            if (l.isEmpty() || l.startsWith("```") || l.startsWith("//")) { continue; }
            if (l.startsWith("- ") || l.startsWith("* ")) { l = l.substring(2).strip(); }
            switch (section) {
                case "entrees" -> champ(l, n, entree, erreurs);
                case "agents" -> agent(l, n, agents, erreurs);
                default -> graphe(l, n, arcs, erreurs);
            }
        }
        if (arcs.isEmpty() && erreurs.isEmpty()) {
            erreurs.add("Il manque la section « ## Graphe » : une ligne par arc, par exemple « entrée -> agent -> fin ».");
        }
        return new ScenarioDef(id, titre == null || titre.isBlank() ? id : titre, entree, agents, arcs,
                description.toString().strip(), fichier, erreurs);
    }

    /** « id : valeur » (texte), « id : a | b | c » (choix, le premier est le défaut), « id (Libellé) : … », « a = Libellé | b = … ». */
    private static void champ(String l, int n, List<ChampEntree> champs, List<String> erreurs) {
        Matcher m = CHAMP.matcher(l);
        if (!m.matches()) {
            erreurs.add("Entrées, ligne " + n + " : attendu « id : valeur » ou « id : a | b | c » (reçu « " + l + " »).");
            return;
        }
        String id = m.group(1);
        String libelle = m.group(2) == null || m.group(2).isBlank() ? id : m.group(2).strip();
        String v = m.group(3).strip();
        if (champs.stream().anyMatch(c -> c.id().equals(id))) {
            erreurs.add("Entrées, ligne " + n + " : le champ « " + id + " » est déclaré deux fois.");
            return;
        }
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            champs.add(new ChampEntree(id, libelle, "texte", v.substring(1, v.length() - 1), List.of()));
        } else if (v.contains("|")) {
            List<ChampEntree.Option> options = new ArrayList<>();
            for (String o : v.split("\\|")) {
                String s = o.strip();
                int e = s.indexOf('=');
                options.add(e < 0 ? new ChampEntree.Option(s, s) : new ChampEntree.Option(s.substring(0, e).strip(), s.substring(e + 1).strip()));
            }
            champs.add(new ChampEntree(id, libelle, "choix", options.get(0).valeur(), options));
        } else {
            champs.add(new ChampEntree(id, libelle, "texte", v, List.of()));
        }
    }

    /** « nom », « nom : outils a, b », « nom = bean : outils a, b » : nom dans le graphe, bean Java, outils autorisés. */
    private static void agent(String l, int n, List<ScenarioDef.Noeud> agents, List<String> erreurs) {
        String gauche = l;
        List<String> outils = new ArrayList<>();
        int c = l.indexOf(':');
        if (c >= 0) {
            gauche = l.substring(0, c).strip();
            String reste = l.substring(c + 1).strip();
            if (norm(reste).startsWith("outils")) { reste = reste.replaceFirst("(?i)^outils?\\s*:?\\s*", ""); }
            for (String o : reste.split(",")) {
                if (!o.isBlank()) { outils.add(o.strip()); }
            }
        }
        String bean;
        String nom;
        int e = gauche.indexOf('=');
        if (e >= 0) {
            nom = norm(gauche.substring(0, e));
            bean = gauche.substring(e + 1).strip();
        } else {
            nom = norm(gauche);
            bean = gauche.strip();
        }
        if (nom.isEmpty() || bean.isEmpty()) {
            erreurs.add("Agents, ligne " + n + " : attendu « nom » ou « nom = bean », éventuellement suivi de « : outils a, b » (reçu « " + l + " »).");
            return;
        }
        if (agents.stream().anyMatch(a -> a.nom().equals(nom))) {
            erreurs.add("Agents, ligne " + n + " : l'agent « " + nom + " » est déclaré deux fois.");
            return;
        }
        agents.add(new ScenarioDef.Noeud(nom, bean, List.copyOf(outils), n));
    }

    /** « a -> b -> c : REJETE, DOUTE (max 2) » ; « a, b -> c » (jonction) ; « a -> b, c » (plusieurs suites) ; « a => b, c » (délégation). */
    private static void graphe(String l, int n, List<ScenarioDef.Arc> arcs, List<String> erreurs) {
        int max = -1;
        Matcher mm = MAX.matcher(l);
        if (mm.find()) {
            max = Integer.parseInt(mm.group(1));
            l = mm.replaceAll("").strip();
        }
        String chaine = l;
        List<String> etiquettes = new ArrayList<>();
        int c = l.indexOf(':');
        if (c >= 0) {
            chaine = l.substring(0, c);
            for (String e : l.substring(c + 1).split(",")) {
                if (!e.isBlank()) { etiquettes.add(e.strip().toUpperCase(Locale.ROOT)); }
            }
        }
        List<List<String>> segments = new ArrayList<>();
        List<Boolean> delegations = new ArrayList<>();
        Matcher f = FLECHE.matcher(chaine);
        int debut = 0;
        while (f.find()) {
            segments.add(noms(chaine.substring(debut, f.start())));
            delegations.add(f.group().equals("=>"));
            debut = f.end();
        }
        segments.add(noms(chaine.substring(debut)));
        if (segments.size() < 2 || segments.stream().anyMatch(List::isEmpty)) {
            erreurs.add("Graphe, ligne " + n + " : attendu « a -> b » (reçu « " + l + " »).");
            return;
        }
        for (int i = 0; i < segments.size() - 1; i++) {
            boolean delegation = delegations.get(i);
            if (delegation && !etiquettes.isEmpty()) {
                erreurs.add("Graphe, ligne " + n + " : un arc « => » ne porte pas d'étiquette (le plan de l'agent choisit les cibles).");
            }
            for (String de : segments.get(i)) {
                for (String vers : segments.get(i + 1)) {
                    arcs.add(new ScenarioDef.Arc(de, vers, List.copyOf(etiquettes), max, delegation, n));
                }
            }
        }
    }

    private static List<String> noms(String s) {
        List<String> r = new ArrayList<>();
        for (String p : s.split(",")) {
            String nom = norm(p);
            if (nom.equals("entree") || nom.equals("debut") || nom.equals("start")) { nom = ScenarioDef.ENTREE; }
            if (!nom.isEmpty()) { r.add(nom); }
        }
        return r;
    }
}
