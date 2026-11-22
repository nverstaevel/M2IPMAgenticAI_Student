package fr.m2ipm.clinique.scenario;

import fr.m2ipm.clinique.ai.OutilsClinique;
import fr.m2ipm.clinique.roles.OutilsAgenda;
import fr.m2ipm.clinique.roles.OutilsBestiaire;
import fr.m2ipm.clinique.roles.OutilsPharmacie;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Catalogue des outils qu'un scénario peut autoriser pour un agent : des groupes (dossier, pharmacie, agenda, bestiaire)
 * ou des outils isolés, désignés par leur nom (par exemple chercherDansWikipedia). Pour ajouter un groupe, ajoutez une ligne ici.
 */
@Component
public class OutilsDisponibles {

    private final Map<String, ToolCallback[]> fixes = new LinkedHashMap<>();
    /** Les outils du jeu, découverts par MCP : interrogés à chaque usage, car le serveur peut démarrer après la clinique. */
    private final Supplier<ToolCallback[]> jeu;

    @Autowired
    public OutilsDisponibles(OutilsClinique dossier, OutilsPharmacie pharmacie, OutilsAgenda agenda, OutilsBestiaire bestiaire,
                             ObjectProvider<ToolCallbackProvider> mcp) {
        fixes.put("dossier", ToolCallbacks.from(dossier));
        fixes.put("pharmacie", ToolCallbacks.from(pharmacie));
        fixes.put("agenda", ToolCallbacks.from(agenda));
        fixes.put("bestiaire", ToolCallbacks.from(bestiaire));
        this.jeu = () -> mcp.orderedStream().flatMap(p -> java.util.Arrays.stream(p.getToolCallbacks())).toArray(ToolCallback[]::new);
    }

    /** Constructeur de test : des groupes quelconques, pas de jeu. */
    public OutilsDisponibles(Map<String, ToolCallback[]> groupes) {
        this(groupes, () -> new ToolCallback[0]);
    }

    /** Constructeur de test avec un « jeu » simulé. */
    public OutilsDisponibles(Map<String, ToolCallback[]> groupes, Supplier<ToolCallback[]> jeu) {
        this.fixes.putAll(groupes);
        this.jeu = jeu;
    }

    /** Les groupes fixes, plus le groupe « jeu » (les outils publiés par le serveur MCP du jeu) quand le client y est connecté. */
    private Map<String, ToolCallback[]> catalogue() {
        Map<String, ToolCallback[]> tous = new LinkedHashMap<>(fixes);
        ToolCallback[] outilsJeu = jeu.get();
        if (outilsJeu.length > 0) { tous.put("jeu", outilsJeu); }
        return tous;
    }

    public List<String> groupes() { return List.copyOf(catalogue().keySet()); }

    /** Noms de tous les outils isolés. */
    public List<String> outils() {
        Set<String> noms = new LinkedHashSet<>();
        catalogue().values().forEach(g -> { for (ToolCallback c : g) { noms.add(c.getToolDefinition().name()); } });
        return List.copyOf(noms);
    }

    public boolean connu(String nom) {
        return catalogue().containsKey(nom.toLowerCase()) || outils().contains(nom);
    }

    /** Les outils désignés, sans doublon. Un nom inconnu est ignoré (le scénario est validé avant). */
    public ToolCallback[] resoudre(List<String> noms) {
        Map<String, ToolCallback> res = new LinkedHashMap<>();
        Map<String, ToolCallback[]> groupes = catalogue();
        for (String nom : noms) {
            ToolCallback[] groupe = groupes.get(nom.toLowerCase());
            if (groupe != null) {
                for (ToolCallback c : groupe) { res.putIfAbsent(c.getToolDefinition().name(), c); }
                continue;
            }
            for (ToolCallback[] g : groupes.values()) {
                for (ToolCallback c : g) {
                    if (c.getToolDefinition().name().equals(nom)) { res.putIfAbsent(nom, c); }
                }
            }
        }
        return new ArrayList<>(res.values()).toArray(new ToolCallback[0]);
    }
}
