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
 *
 * 🧠 À COMPLÉTER dans ce fichier : 2 zones marquées « TODO » (TD1). Tout le reste est fourni et testé : lisez-le, ne le modifiez pas.
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

    /** Méthode utilitaire pour les pages web (non imposée par le sujet). */
    public CreatureDto trouverCreature(Long id) {
        return versDto(creature(id));
    }

    // ---------------------------------------------------------------- écritures

    /**
     * Planifie une consultation de 30 minutes. À ÉCRIRE : c'est la règle de gestion du TD.
     *   1. récupérer la créature : creature(creatureId) lève déjà une RessourceIntrouvableException claire ;
     *   2. récupérer le soigneur AVEC le verrou : soigneurs.findByIdPourPlanification(soigneurId)
     *      (Optional : s'il est vide, lever RessourceIntrouvableException « Aucun soigneur d'identifiant … ») ;
     *   3. détecter le chevauchement : consultations.existsBySoigneurIdAndDateHeureGreaterThanAndDateHeureLessThan(
     *        soigneurId, borneBasse, borneHaute) est vrai s'il existe une consultation du soigneur qui commence
     *      strictement entre les deux bornes. Quelles bornes (en fonction de dateHeure et de DUREE_MINUTES) ?
     *      Réfléchissez avec l'exemple du sujet : 14 h 00 puis 14 h 10 chez le même soigneur ;
     *   4. en cas de conflit : lever ConflitDeCreneauException avec une phrase que comprend un humain ET un modèle,
     *      par exemple « Le soigneur 3 (Dr Gribouille) est déjà pris le 12/03 à 14h00 : une consultation dure 30 minutes. »
     *      (FORMAT_CRENEAU formate la date) ;
     *   5. sinon : consultations.save(new Consultation(dateHeure, creature, soigneur, motif, TARIF_PAR_DEFAUT))
     *      et renvoyer versDto(...).
     */
    @Transactional
    public ConsultationDto planifierConsultation(Long creatureId, Long soigneurId,
                                                 LocalDateTime dateHeure, String motif) {
        // TODO TD1 : à écrire
        throw new UnsupportedOperationException("À écrire : planifierConsultation");
    }

    /**
     * Clôt une consultation. À ÉCRIRE : retrouver la consultation (sinon RessourceIntrouvableException
     * « Aucune consultation d'identifiant … »), refuser un second diagnostic (OperationInvalideException
     * « La consultation … a déjà un diagnostic. »), puis c.cloturer(diagnostic, traitement).
     * Pas de save() : l'entité est gérée, la transaction enregistre la modification.
     */
    @Transactional
    public void enregistrerDiagnostic(Long consultationId, String diagnostic, String traitement) {
        // TODO TD1 : à écrire
        throw new UnsupportedOperationException("À écrire : enregistrerDiagnostic");
    }

    // ---------------------------------------------------------------- utilitaires

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
