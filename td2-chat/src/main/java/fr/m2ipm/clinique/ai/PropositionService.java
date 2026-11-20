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
                                                // TODO TD2 : à écrire
                                                throw new UnsupportedOperationException("À écrire : proposer");
                                            }

    /** Propositions non expirées d'une conversation. */
    public List<PropositionConsultation> enAttente(String conversationId) {
        // TODO TD2 : à écrire
        throw new UnsupportedOperationException("À écrire : enAttente");
    }

    /** Oublie les propositions en attente d'une conversation (réinitialisation du chat). N'écrit rien en base. */
    public void oublier(String conversationId) {
        // TODO TD2 : à écrire
        throw new UnsupportedOperationException("À écrire : oublier");
    }

    /** Crée réellement la consultation. À n'appeler que depuis un endpoint réservé au personnel. */
    public ConsultationDto confirmer(String id) {
        // TODO TD2 : à écrire
        throw new UnsupportedOperationException("À écrire : confirmer");
    }

    private void purger() {
        // TODO TD2 : à écrire
        throw new UnsupportedOperationException("À écrire : purger");
    }
}
