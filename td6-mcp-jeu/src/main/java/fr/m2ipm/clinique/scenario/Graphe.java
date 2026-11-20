package fr.m2ipm.clinique.scenario;

import fr.m2ipm.clinique.scenario.ScenarioDef.Arc;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Analyse du graphe d'un scénario : nœuds, arcs entrants et sortants, arcs « retour » (boucles) et corps de chaque boucle. */
final class Graphe {

    private final List<Arc> arcs;
    private final List<String> noeuds = new ArrayList<>();
    private final Set<Arc> retours = new HashSet<>();

    private Graphe(List<Arc> arcs) {
        this.arcs = arcs;
        for (Arc a : arcs) {
            for (String n : List.of(a.de(), a.vers())) {
                if (!n.equals(ScenarioDef.ENTREE) && !noeuds.contains(n)) { noeuds.add(n); }
            }
        }
        classer();
    }

    static Graphe de(ScenarioDef d) { return new Graphe(d.arcs()); }

    /** Nœuds du graphe, entrée exclue, dans l'ordre où ils apparaissent dans le fichier. */
    List<String> noeuds() { return noeuds; }

    List<Arc> sortants(String n) { return arcs.stream().filter(a -> a.de().equals(n)).toList(); }

    List<Arc> entrants(String n) { return arcs.stream().filter(a -> a.vers().equals(n)).toList(); }

    /** Arcs entrants qui ne sont pas des retours en arrière. */
    List<Arc> entrantsAvant(String n) { return entrants(n).stream().filter(a -> !retours.contains(a)).toList(); }

    boolean estRetour(Arc a) { return retours.contains(a); }

    Set<Arc> retours() { return retours; }

    boolean dansBoucle(String n) {
        return retours.stream().anyMatch(r -> corps(r).contains(n));
    }

    /** Nœuds atteignables depuis l'entrée en suivant tous les arcs. */
    Set<String> accessibles() {
        return atteints(ScenarioDef.ENTREE, arcs, true);
    }

    /** Corps de la boucle fermée par un arc retour s -> t : t, s et les nœuds qui sont sur un chemin avant de t vers s. */
    Set<String> corps(Arc retour) {
        List<Arc> avant = arcs.stream().filter(a -> !retours.contains(a)).toList();
        Set<String> depuisT = atteints(retour.vers(), avant, true);
        Set<String> versS = atteints(retour.de(), avant, false);
        Set<String> corps = new LinkedHashSet<>();
        for (String n : depuisT) { if (versS.contains(n)) { corps.add(n); } }
        corps.add(retour.vers());
        corps.add(retour.de());
        return corps;
    }

    private static Set<String> atteints(String depart, List<Arc> arcs, boolean sens) {
        Set<String> vus = new LinkedHashSet<>(List.of(depart));
        ArrayDeque<String> file = new ArrayDeque<>(List.of(depart));
        while (!file.isEmpty()) {
            String n = file.poll();
            for (Arc a : arcs) {
                String suivant = sens ? (a.de().equals(n) ? a.vers() : null) : (a.vers().equals(n) ? a.de() : null);
                if (suivant != null && vus.add(suivant)) { file.add(suivant); }
            }
        }
        return vus;
    }

    /** Un arc est un retour s'il mène à un nœud en cours de parcours (parcours en profondeur depuis l'entrée). */
    private void classer() {
        Set<String> pile = new HashSet<>();
        Set<String> vus = new HashSet<>();
        parcourir(ScenarioDef.ENTREE, pile, vus);
    }

    private void parcourir(String n, Set<String> pile, Set<String> vus) {
        vus.add(n);
        pile.add(n);
        for (Arc a : sortants(n)) {
            if (pile.contains(a.vers())) {
                retours.add(a);
            } else if (!vus.contains(a.vers())) {
                parcourir(a.vers(), pile, vus);
            }
        }
        pile.remove(n);
    }
}
