package fr.m2ipm.clinique.ai;

import fr.m2ipm.clinique.dto.*;
import fr.m2ipm.clinique.service.CliniqueService;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Outils exposés au modèle : cinq lectures et une proposition d'écriture.
 * Il n'existe volontairement aucun outil qui confirme ou crée une consultation.
 */
@Component
public class OutilsClinique {

    private final CliniqueService service;
    private final PropositionService propositions;

    public OutilsClinique(CliniqueService service, PropositionService propositions) {
        this.service = service;
        this.propositions = propositions;
    }

    @Tool(description = "Cherche les propriétaires dont le nom de famille contient le texte donné "
            + "(texte vide = tous les propriétaires). Renvoie id, nom, prénom et ville.")
    public List<ProprietaireVue> rechercherProprietaires(
            @ToolParam(description = "Partie du nom de famille, sans tenir compte des majuscules") String nom) {
                // TODO TD2 : à écrire
                throw new UnsupportedOperationException("À écrire : rechercherProprietaires");
            }

    @Tool(description = "Donne la fiche d'un propriétaire (id, nom, prénom, ville) à partir de son identifiant.")
    public ProprietaireVue trouverProprietaire(@ToolParam(description = "Identifiant du propriétaire") Long id) {
        // TODO TD2 : à écrire
        throw new UnsupportedOperationException("À écrire : trouverProprietaire");
    }

    @Tool(description = "Liste les créatures d'un propriétaire : id, nom, date de naissance, espèce.")
    public List<CreatureDto> creaturesDuProprietaire(
            @ToolParam(description = "Identifiant du propriétaire") Long proprietaireId) {
                // TODO TD2 : à écrire
                throw new UnsupportedOperationException("À écrire : creaturesDuProprietaire");
            }

    @Tool(description = "Historique des consultations d'une créature, la plus récente d'abord : date, soigneur, "
            + "motif, diagnostic et traitement (vides si la consultation n'a pas encore eu lieu).")
    public List<ConsultationDto> consultationsDeLaCreature(
            @ToolParam(description = "Identifiant de la créature") Long creatureId) {
                // TODO TD2 : à écrire
                throw new UnsupportedOperationException("À écrire : consultationsDeLaCreature");
            }

    @Tool(description = "Liste les soigneurs compétents pour une espèce (ex. Dragon, Phénix, Yéti).")
    public List<SoigneurDto> soigneursPourEspece(
            @ToolParam(description = "Nom de l'espèce") String espece) {
                // TODO TD2 : à écrire
                throw new UnsupportedOperationException("À écrire : soigneursPourEspece");
            }

    @Tool(description = "PROPOSE une consultation sans la créer : un membre du personnel devra la confirmer. "
            + "Vérifie que la créature et le soigneur existent et que le créneau est libre (30 minutes). "
            + "Renvoie un résumé de la proposition ou un message d'erreur à transmettre à l'utilisateur.")
    public String proposerConsultation(
            @ToolParam(description = "Identifiant de la créature") Long creatureId,
            @ToolParam(description = "Identifiant du soigneur") Long soigneurId,
            @ToolParam(description = "Date et heure au format ISO 8601, par exemple 2026-12-03T14:30:00") String dateHeure,
            @ToolParam(description = "Motif de la consultation") String motif,
            ToolContext contexte) {
                // TODO TD2 : à écrire
                throw new UnsupportedOperationException("À écrire : proposerConsultation");
            }

    private static ProprietaireVue vue(ProprietaireDto p) {
        // TODO TD2 : à écrire
        throw new UnsupportedOperationException("À écrire : vue");
    }
}
