package fr.m2ipm.clinique.externe;

import fr.m2ipm.clinique.dto.CreatureDto;
import fr.m2ipm.clinique.service.CliniqueService;
import fr.m2ipm.clinique.service.ConflitDeCreneauException;
import fr.m2ipm.clinique.service.OperationInvalideException;
import fr.m2ipm.clinique.service.RessourceIntrouvableException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Même principe qu'au TD2 : le modèle propose, seul un endpoint réservé au personnel écrit dans l'agenda. */
@Service
public class PropositionAgendaService {

    private final CliniqueService clinique;
    private final AgendaPort agenda;
    private final Clock horloge;
    private final Duration duree;
    private final Map<String, PropositionAgenda> propositions = new ConcurrentHashMap<>();

    @Autowired
    public PropositionAgendaService(CliniqueService clinique, AgendaPort agenda,
                                    @Value("${clinique.proposition.ttl:PT10M}") Duration duree) {
        this(clinique, agenda, Clock.systemDefaultZone(), duree);
    }

    public PropositionAgendaService(CliniqueService clinique, AgendaPort agenda, Clock horloge, Duration duree) {
        this.clinique = clinique;
        this.agenda = agenda;
        this.horloge = horloge;
        this.duree = duree;
    }

    /** Vérifie la créature et que le créneau est libre dans l'agenda, puis mémorise. N'écrit rien dans l'agenda. */
    public PropositionAgenda proposer(String conversationId, Long creatureId, LocalDateTime dateHeure, String motif) {
        CreatureDto creature = clinique.trouverCreature(creatureId);
        if (!agenda.creneauxLibres(dateHeure.toLocalDate()).contains(dateHeure.toLocalTime())) {
            throw new ConflitDeCreneauException("Le créneau du " + dateHeure + " n'est pas libre dans l'agenda.");
        }
        String titre = "Consultation — " + creature.nom() + " (" + creature.espece() + ")";
        var p = new PropositionAgenda(UUID.randomUUID().toString(), conversationId, creatureId, titre, dateHeure, motif,
                Instant.now(horloge).plus(duree));
        propositions.put(p.id(), p);
        return p;
    }

    public List<PropositionAgenda> enAttente(String conversationId) {
        Instant maintenant = Instant.now(horloge);
        propositions.values().removeIf(p -> p.expiration().isBefore(maintenant));
        return propositions.values().stream()
                .filter(p -> p.conversationId().equals(conversationId))
                .sorted(Comparator.comparing(PropositionAgenda::expiration))
                .toList();
    }

    /** Crée l'événement (30 minutes, sans invité) et renvoie son identifiant. Réservé au personnel côté HTTP. */
    public String confirmer(String id) {
        PropositionAgenda p = propositions.get(id);
        if (p == null) {
            throw new RessourceIntrouvableException("Aucune proposition d'agenda d'identifiant " + id);
        }
        if (p.expiration().isBefore(Instant.now(horloge))) {
            propositions.remove(id);
            throw new OperationInvalideException("La proposition " + id + " a expiré : demandez-en une nouvelle.");
        }
        String evenement = agenda.creerEvenement(p.titre(), p.dateHeure(), 30);
        propositions.remove(id);
        return evenement;
    }
}
