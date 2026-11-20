package fr.m2ipm.clinique.ai;

import fr.m2ipm.clinique.dto.ConsultationDto;
import fr.m2ipm.clinique.service.CliniqueService;
import fr.m2ipm.clinique.service.OperationInvalideException;
import fr.m2ipm.clinique.service.RessourceIntrouvableException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Propositions de consultation : le modèle propose, un humain confirme.
 * Aucun outil exposé au modèle n'appelle {@link #confirmer(String)}.
 */
@Service
public class PropositionService {

    private final CliniqueService clinique;
    private final Clock horloge;
    private final Duration duree;
    private final Map<String, PropositionConsultation> propositions = new ConcurrentHashMap<>();

    @Autowired
    public PropositionService(CliniqueService clinique, @Value("${clinique.proposition.ttl:PT10M}") Duration duree) {
        this(clinique, Clock.systemDefaultZone(), duree);
    }

    /** Constructeur de test : horloge injectable. */
    public PropositionService(CliniqueService clinique, Clock horloge, Duration duree) {
        this.clinique = clinique;
        this.horloge = horloge;
        this.duree = duree;
    }

    /** Vérifie les données (créature, soigneur, créneau libre) puis mémorise la proposition. N'écrit rien en base. */
    public PropositionConsultation proposer(String conversationId, Long creatureId, Long soigneurId,
                                            LocalDateTime dateHeure, String motif) {
        var creature = clinique.trouverCreature(creatureId);
        var soigneur = clinique.trouverSoigneur(soigneurId);
        clinique.verifierDisponibilite(soigneurId, dateHeure);
        var p = new PropositionConsultation(UUID.randomUUID().toString(), conversationId, creatureId, creature.nom(),
                soigneurId, soigneur.nom(), dateHeure, motif, Instant.now(horloge).plus(duree));
        propositions.put(p.id(), p);
        return p;
    }

    /** Propositions non expirées d'une conversation. */
    public List<PropositionConsultation> enAttente(String conversationId) {
        purger();
        return propositions.values().stream()
                .filter(p -> p.conversationId().equals(conversationId))
                .sorted(java.util.Comparator.comparing(PropositionConsultation::expiration))
                .toList();
    }

    /** Crée réellement la consultation. À n'appeler que depuis un endpoint réservé au personnel. */
    public ConsultationDto confirmer(String id) {
        PropositionConsultation p = propositions.get(id);
        if (p == null) {
            throw new RessourceIntrouvableException("Aucune proposition d'identifiant " + id);
        }
        if (p.expiration().isBefore(Instant.now(horloge))) {
            propositions.remove(id);
            throw new OperationInvalideException("La proposition " + id + " a expiré : demandez-en une nouvelle.");
        }
        // Si le créneau a été pris entre-temps, planifierConsultation lève ConflitDeCreneauException
        // et la proposition reste en attente (le personnel peut la refuser ou en demander une autre).
        ConsultationDto c = clinique.planifierConsultation(p.creatureId(), p.soigneurId(), p.dateHeure(), p.motif());
        propositions.remove(id);
        return c;
    }

    private void purger() {
        Instant maintenant = Instant.now(horloge);
        propositions.values().removeIf(p -> p.expiration().isBefore(maintenant));
    }
}
