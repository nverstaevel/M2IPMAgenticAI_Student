package fr.m2ipm.jeu;

import fr.m2ipm.jeu.Jeu.Compte;
import fr.m2ipm.jeu.Jeu.Etat;
import fr.m2ipm.jeu.Jeu.Fiche;
import fr.m2ipm.jeu.Jeu.LignePharmacie;
import fr.m2ipm.jeu.Jeu.Observation;
import fr.m2ipm.jeu.Jeu.PatientVue;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Ce que le jeu publie par MCP. Chaque méthode annotée {@code @Tool} devient un outil que n'importe quel client MCP
 * (votre agent, MCP Inspector, Claude Desktop…) découvre avec son nom, sa description et son schéma d'arguments.
 */
@Component
public class OutilsJeu {

    private final PartieService parties;

    public OutilsJeu(PartieService parties) { this.parties = parties; }

    @Tool(name = "etat_clinique", description = "État de la clinique : jour, points d'action restants aujourd'hui, trésorerie, réputation, guérisons, décès, score. Gratuit.")
    public Etat etatClinique() { return parties.jeu().etat(); }

    @Tool(name = "lister_patients", description = "Patients présents : identifiant, espèce, points de vie (PV), urgence, symptômes visibles, signe distinctif (seulement après examen) et note du propriétaire. Gratuit.")
    public List<PatientVue> listerPatients() { return parties.jeu().patients(); }

    @Tool(name = "fiche_espece", description = "Fiche du bestiaire pour une espèce (dragonnet, loup-garou, vampire, licorne) : ses maladies, leur signe distinctif, le remède de chacune et les médicaments toxiques. Gratuit.")
    public Fiche ficheEspece(@ToolParam(description = "Nom de l'espèce, par exemple vampire") String espece) { return parties.jeu().fiche(espece); }

    @Tool(name = "carnet_soins", description = "Carnet de soins : les effets de soins déjà observés dans cette partie (espèce, médicament, effet GUERISON / TOXIQUE / REVIGORANT / AUCUN_EFFET, maladie ou signe distinctif concerné, nombre de fois). Mémoire de la clinique : ne contient que ce qui a été essayé. Gratuit.")
    public List<Observation> carnetSoins() { return parties.jeu().carnet(); }

    @Tool(name = "consulter_pharmacie", description = "Stock de la pharmacie : prix unitaire, quantité en stock, quantité en commande (livrée à la fin de la journée). Gratuit.")
    public List<LignePharmacie> consulterPharmacie() { return parties.jeu().pharmacie(); }

    @Tool(name = "examiner_patient", description = "Examine un patient pour révéler son signe distinctif, qui permet de distinguer ses deux maladies possibles. Coûte 1 point d'action et 5 de trésorerie.")
    public Compte examinerPatient(@ToolParam(description = "Identifiant du patient, par exemple P03") String idPatient) { return parties.jeu().examiner(idPatient); }

    @Tool(name = "soigner", description = "Administre un médicament à un patient. Le bon remède le guérit (honoraires 80) ; un médicament toxique pour son espèce peut le tuer ; un autre n'a aucun effet. Consomme une unité de stock et 1 point d'action.")
    public Compte soigner(@ToolParam(description = "Identifiant du patient") String idPatient,
                          @ToolParam(description = "Nom exact du médicament, tel que listé par consulter_pharmacie") String medicament) {
        return parties.jeu().soigner(idPatient, medicament);
    }

    @Tool(name = "commander", description = "Commande un médicament (1 à 20 unités), payé tout de suite, livré à la fin de la journée. Gratuit en points d'action.")
    public Compte commander(@ToolParam(description = "Nom exact du médicament") String medicament,
                            @ToolParam(description = "Quantité, de 1 à 20") int quantite) {
        return parties.jeu().commander(medicament, quantite);
    }

    @Tool(name = "fin_de_journee", description = "Termine la journée : les patients non soignés perdent 15 PV (ils meurent à 0), les commandes sont livrées, de nouveaux patients arrivent, les points d'action sont rétablis. Après le dernier jour, la partie est terminée.")
    public Compte finDeJournee() { return parties.jeu().finDeJournee(); }
}
