package fr.m2ipm.clinique.roles;

import fr.m2ipm.clinique.externe.OutilsExternes;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** Sous-ensemble d'outils : pharmacie, catalogue et tarifs (rien d'autre). */
@Component
public class OutilsPharmacie {

    private final OutilsExternes externes;

    public OutilsPharmacie(OutilsExternes externes) { this.externes = externes; }

    @Tool(description = "Prix en euros d'un acte (Consultation, Vaccin, Soin de griffes, Examen de nuit) pour une espèce.")
    public String consulterTarif(@ToolParam(description = "Nom de l'acte") String acte,
                                 @ToolParam(description = "Nom de l'espèce") String espece) {
        return externes.consulterTarif(acte, espece);
    }

    @Tool(description = "Stock disponible d'un médicament dans la pharmacie de la clinique.")
    public String stockPharmacie(@ToolParam(description = "Nom du médicament") String medicament) {
        return externes.stockPharmacie(medicament);
    }

    @Tool(description = "Fiche du catalogue de médicaments pour une espèce : principe actif, contre-indications, dose.")
    public String chercherMedicament(@ToolParam(description = "Nom (ou début de nom)") String nom,
                                     @ToolParam(description = "Nom de l'espèce") String espece) {
        return externes.chercherMedicament(nom, espece);
    }
}
