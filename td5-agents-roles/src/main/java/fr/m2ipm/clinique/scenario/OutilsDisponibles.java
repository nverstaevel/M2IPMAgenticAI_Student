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

    private final Map<String, ToolCallback[]> groupes = new LinkedHashMap<>();

    @Autowired
    public OutilsDisponibles(OutilsClinique dossier, OutilsPharmacie pharmacie, OutilsAgenda agenda, OutilsBestiaire bestiaire) {
        groupes.put("dossier", ToolCallbacks.from(dossier));
        groupes.put("pharmacie", ToolCallbacks.from(pharmacie));
        groupes.put("agenda", ToolCallbacks.from(agenda));
        groupes.put("bestiaire", ToolCallbacks.from(bestiaire));
    }

    /** Constructeur de test : des groupes quelconques. */
    public OutilsDisponibles(Map<String, ToolCallback[]> groupes) {
        this.groupes.putAll(groupes);
    }

    public List<String> groupes() { return List.copyOf(groupes.keySet()); }

    /** Noms de tous les outils isolés. */
    public List<String> outils() {
        Set<String> noms = new LinkedHashSet<>();
        groupes.values().forEach(g -> { for (ToolCallback c : g) { noms.add(c.getToolDefinition().name()); } });
        return List.copyOf(noms);
    }

    public boolean connu(String nom) {
        return groupes.containsKey(nom.toLowerCase()) || outils().contains(nom);
    }

    /** Les outils désignés, sans doublon. Un nom inconnu est ignoré (le scénario est validé avant). */
    public ToolCallback[] resoudre(List<String> noms) {
        Map<String, ToolCallback> res = new LinkedHashMap<>();
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
