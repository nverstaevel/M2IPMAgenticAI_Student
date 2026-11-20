package fr.m2ipm.clinique.scenario;

import java.util.List;
import java.util.Optional;

/**
 * Un scénario lu dans un fichier .md : un titre, des entrées, des agents (avec leurs outils autorisés) et un graphe d'arcs.
 * Les noms de nœuds sont normalisés (minuscules, sans accent, tirets) ; « entree », « fin » et « humain » sont réservés.
 */
public record ScenarioDef(String id, String titre, List<ChampEntree> entree, List<Noeud> agents, List<Arc> arcs,
                          String description, String fichier, List<String> erreurs) {

    public static final String ENTREE = "entree";
    public static final String FIN = "fin";
    public static final String HUMAIN = "humain";

    /** Un agent du graphe : son nom dans le graphe, le bean Java qui l'exécute, les outils qu'il a le droit d'utiliser. */
    public record Noeud(String nom, String bean, List<String> outils, int ligne) {}

    /**
     * Un arc « de -> vers ». etiquettes : conditions (APPROUVE, REJETE… en majuscules), vide = toujours.
     * max : nombre de passages autorisés quand l'arc est un retour en arrière (-1 = valeur par défaut).
     * delegation : « => », le plan de l'agent source désigne les cibles.
     */
    public record Arc(String de, String vers, List<String> etiquettes, int max, boolean delegation, int ligne) {
        public boolean etiquete() { return !etiquettes.isEmpty(); }
    }

    public ScenarioDef avecErreurs(List<String> e) {
        return new ScenarioDef(id, titre, entree, agents, arcs, description, fichier, List.copyOf(e));
    }

    public boolean valide() { return erreurs.isEmpty(); }

    public Optional<Noeud> noeud(String nom) {
        return agents.stream().filter(n -> n.nom().equals(nom)).findFirst();
    }

    public static boolean reserve(String nom) {
        return nom.equals(ENTREE) || nom.equals(FIN) || nom.equals(HUMAIN);
    }
}
