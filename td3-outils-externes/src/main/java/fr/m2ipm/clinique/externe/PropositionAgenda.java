package fr.m2ipm.clinique.externe;

import java.time.Instant;
import java.time.LocalDateTime;

/** Un événement d'agenda proposé par le modèle, en attente de confirmation humaine. Rien n'est écrit dans l'agenda. */
public record PropositionAgenda(String id, String conversationId, Long creatureId, String titre,
                                LocalDateTime dateHeure, String motif, Instant expiration) {}
