package fr.m2ipm.clinique.service;

import fr.m2ipm.clinique.domain.*;
import fr.m2ipm.clinique.dto.*;
import fr.m2ipm.clinique.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Couche service de la clinique. Les signatures publiques sont imposées par le sujet :
 * les TD suivants les exposeront au modèle de langage sous forme d'outils.
 */
@Service
@Transactional(readOnly = true)
public class CliniqueService {

    private static final BigDecimal TARIF_PAR_DEFAUT = new BigDecimal("40.00");
    private static final DateTimeFormatter FORMAT_CRENEAU =
            DateTimeFormatter.ofPattern("dd/MM 'à' HH'h'mm", Locale.FRANCE);

    private final ProprietaireRepository proprietaires;
    private final CreatureRepository creatures;
    private final SoigneurRepository soigneurs;
    private final ConsultationRepository consultations;

    public CliniqueService(ProprietaireRepository proprietaires, CreatureRepository creatures,
                           SoigneurRepository soigneurs, ConsultationRepository consultations) {
        this.proprietaires = proprietaires;
        this.creatures = creatures;
        this.soigneurs = soigneurs;
        this.consultations = consultations;
    }

    // ---------------------------------------------------------------- lectures

    /** Recherche partielle, insensible à la casse. Une chaîne vide renvoie tous les propriétaires. */
    public List<ProprietaireDto> rechercherProprietaires(String nom) {
        String motif = nom == null ? "" : nom.trim();
        return proprietaires.findByNomContainingIgnoreCaseOrderByNom(motif).stream()
                .map(CliniqueService::versDto).toList();
    }

    public ProprietaireDto trouverProprietaire(Long id) {
        return versDto(proprietaire(id));
    }

    public List<CreatureDto> creaturesDuProprietaire(Long proprietaireId) {
        proprietaire(proprietaireId); // 404 explicite si le propriétaire n'existe pas
        return creatures.findByProprietaireIdOrderByNom(proprietaireId).stream()
                .map(CliniqueService::versDto).toList();
    }

    /** Historique d'une créature, consultation la plus récente d'abord. */
    public List<ConsultationDto> consultationsDeLaCreature(Long creatureId) {
        creature(creatureId);
        return consultations.findByCreatureIdOrderByDateHeureDesc(creatureId).stream()
                .map(CliniqueService::versDto).toList();
    }

    public List<SoigneurDto> soigneursPourEspece(String espece) {
        String nom = espece == null ? "" : espece.trim();
        return soigneurs.findByEspece(nom).stream().map(CliniqueService::versDto).toList();
    }

    public SoigneurDto trouverSoigneur(Long id) {
        return versDto(soigneur(id));
    }

    /**
     * Vérifie, sans rien écrire, qu'un soigneur est libre à la date donnée.
     * @throws ConflitDeCreneauException si un autre rendez-vous chevauche le créneau
     */
    public void verifierDisponibilite(Long soigneurId, LocalDateTime dateHeure) {
        verifierDisponibilite(soigneur(soigneurId), dateHeure);
    }

    /** Méthode utilitaire pour les pages web (non imposée par le sujet). */
    public CreatureDto trouverCreature(Long id) {
        return versDto(creature(id));
    }

    // ---------------------------------------------------------------- écritures

    @Transactional
    public ConsultationDto planifierConsultation(Long creatureId, Long soigneurId,
                                                 LocalDateTime dateHeure, String motif) {
        Creature creature = creature(creatureId);
        // Verrou pessimiste sur le soigneur : deux requêtes simultanées pour le même
        // soigneur sont sérialisées, la seconde voit donc la première.
        Soigneur soigneur = soigneurs.findByIdPourPlanification(soigneurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Aucun soigneur d'identifiant " + soigneurId));
        verifierDisponibilite(soigneur, dateHeure);
        Consultation c = consultations.save(new Consultation(dateHeure, creature, soigneur, motif, TARIF_PAR_DEFAUT));
        return versDto(c);
    }

    @Transactional
    public void enregistrerDiagnostic(Long consultationId, String diagnostic, String traitement) {
        Consultation c = consultations.findById(consultationId).orElseThrow(
                () -> new RessourceIntrouvableException("Aucune consultation d'identifiant " + consultationId));
        if (c.getDiagnostic() != null) {
            throw new OperationInvalideException("La consultation " + consultationId + " a déjà un diagnostic.");
        }
        c.cloturer(diagnostic, traitement);
    }

    // ---------------------------------------------------------------- utilitaires

    private void verifierDisponibilite(Soigneur soigneur, LocalDateTime dateHeure) {
        // Deux consultations de 30 min se chevauchent si leurs débuts sont distants de moins de 30 min.
        boolean conflit = consultations.existsBySoigneurIdAndDateHeureGreaterThanAndDateHeureLessThan(
                soigneur.getId(),
                dateHeure.minusMinutes(Consultation.DUREE_MINUTES),
                dateHeure.plusMinutes(Consultation.DUREE_MINUTES));
        if (conflit) {
            throw new ConflitDeCreneauException("Le soigneur " + soigneur.getId() + " (" + soigneur.getNom()
                    + ") est déjà pris le " + FORMAT_CRENEAU.format(dateHeure)
                    + " : une consultation dure " + Consultation.DUREE_MINUTES + " minutes.");
        }
    }

    private Soigneur soigneur(Long id) {
        return soigneurs.findById(id).orElseThrow(
                () -> new RessourceIntrouvableException("Aucun soigneur d'identifiant " + id));
    }

    private Proprietaire proprietaire(Long id) {
        return proprietaires.findById(id).orElseThrow(
                () -> new RessourceIntrouvableException("Aucun propriétaire d'identifiant " + id));
    }

    private Creature creature(Long id) {
        return creatures.findById(id).orElseThrow(
                () -> new RessourceIntrouvableException("Aucune créature d'identifiant " + id));
    }

    private static ProprietaireDto versDto(Proprietaire p) {
        return new ProprietaireDto(p.getId(), p.getNom(), p.getPrenom(), p.getVille(), p.getTelephone());
    }

    private static CreatureDto versDto(Creature c) {
        return new CreatureDto(c.getId(), c.getNom(), c.getDateNaissance(),
                c.getEspece().getNom(), c.getProprietaire().getId());
    }

    private static SoigneurDto versDto(Soigneur s) {
        return new SoigneurDto(s.getId(), s.getNom(),
                s.getEspecesSoignees().stream().map(Espece::getNom).sorted().toList());
    }

    private static ConsultationDto versDto(Consultation c) {
        return new ConsultationDto(c.getId(), c.getDateHeure(), c.getCreature().getId(), c.getCreature().getNom(),
                c.getSoigneur().getId(), c.getSoigneur().getNom(), c.getMotif(),
                c.getDiagnostic(), c.getTraitement(), c.getTarif());
    }
}
