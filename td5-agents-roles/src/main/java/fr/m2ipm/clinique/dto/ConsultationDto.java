package fr.m2ipm.clinique.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConsultationDto(Long id, LocalDateTime dateHeure, Long creatureId, String creatureNom,
                              Long soigneurId, String soigneurNom, String motif,
                              String diagnostic, String traitement, BigDecimal tarif) {}
