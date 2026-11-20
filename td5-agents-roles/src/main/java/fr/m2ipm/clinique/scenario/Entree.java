package fr.m2ipm.clinique.scenario;

import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.ToolCallback;

/**
 * Ce que l'orchestrateur donne à un agent.
 *
 * @param runId       identifiant de l'exécution (utile pour le contexte des outils)
 * @param noeud       le nom du nœud dans le graphe du scénario (deux nœuds peuvent partager le même bean)
 * @param id          l'identifiant de ce passage dans le graphe (« gardien », puis « gardien-2 » à la reprise d'une boucle)
 * @param texte       l'entrée principale : la demande, la production de l'agent précédent, la consigne d'une tâche déléguée…
 * @param donnees     les champs saisis dans la page (la section « Entrées » du .md), tous sous forme de texte
 * @param retours     remarques du critique quand une boucle reprend ce nœud, vide au premier passage
 * @param tour        numéro du passage dans ce nœud (à partir de 1)
 * @param proposition résultat de l'agent précédent (pour un critique) ou, après une jonction, les réponses de tous les agents amont
 * @param outils      les seuls outils que le scénario autorise pour ce nœud (déjà suivis dans le graphe) : à passer au client avec
 *                    {@code .toolCallbacks(entree.outilsAutorises())}. Vide si le .md n'en déclare aucun.
 * @param trace       pour ajouter des nœuds au graphe
 */
public record Entree(String runId, String noeud, String id, String texte, Map<String, String> donnees, List<String> retours, int tour,
                     Resultat proposition, List<ToolCallback> outils, Trace trace) {

    public Entree {
        donnees = donnees == null ? Map.of() : Map.copyOf(donnees);
        retours = retours == null ? List.of() : List.copyOf(retours);
        outils = outils == null ? List.of() : List.copyOf(outils);
    }

    public String donnee(String cle, String defaut) {
        String v = donnees.get(cle);
        return v == null || v.isBlank() ? defaut : v;
    }

    /** Les outils autorisés, sous la forme attendue par {@code ChatClient.prompt().toolCallbacks(...)}. */
    public ToolCallback[] outilsAutorises() {
        return outils.toArray(new ToolCallback[0]);
    }

    Entree avecTraceEtOutils(Trace t, List<ToolCallback> o) {
        return new Entree(runId, noeud, id, texte, donnees, retours, tour, proposition, o, t);
    }
}
