package fr.m2ipm.clinique.ai;

import java.time.Instant;
import java.time.LocalDateTime;

/** Une consultation proposée par le modèle, en attente de confirmation humaine. Rien n'est écrit en base. */
public record PropositionConsultation(String id, String conversationId, Long creatureId, String creatureNom,
                                      Long soigneurId, String soigneurNom, LocalDateTime dateHeure,
                                      String motif, Instant expiration) {}
