package fr.m2ipm.clinique.roles;

import fr.m2ipm.clinique.externe.OutilsExternes;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** Sous-ensemble d'outils : agenda (lecture et proposition seulement). */
@Component
public class OutilsAgenda {

    private final OutilsExternes externes;

    public OutilsAgenda(OutilsExternes externes) { this.externes = externes; }

    @Tool(description = "Créneaux libres de 30 minutes (9h à 18h) dans l'agenda pour un jour donné.")
    public String creneauxLibres(@ToolParam(description = "Jour au format AAAA-MM-JJ") String jour) {
        return externes.creneauxLibres(jour);
    }

    @Tool(description = "PROPOSE un événement d'agenda pour une consultation, sans le créer : le personnel confirme.")
    public String proposerEvenementAgenda(@ToolParam(description = "Identifiant de la créature") Long creatureId,
                                          @ToolParam(description = "Date et heure ISO 8601, ex. 2026-12-03T14:30:00") String dateHeure,
                                          @ToolParam(description = "Motif") String motif,
                                          ToolContext contexte) {
        return externes.proposerEvenementAgenda(creatureId, dateHeure, motif, contexte);
    }
}
