package fr.m2ipm.clinique.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record PlanificationRequest(
        @NotNull(message = "creatureId est obligatoire") Long creatureId,
        @NotNull(message = "soigneurId est obligatoire") Long soigneurId,
        @NotNull(message = "dateHeure est obligatoire") LocalDateTime dateHeure,
        @NotBlank(message = "motif est obligatoire") String motif) {}
