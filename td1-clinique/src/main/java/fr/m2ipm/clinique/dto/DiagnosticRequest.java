package fr.m2ipm.clinique.dto;

import jakarta.validation.constraints.NotBlank;

public record DiagnosticRequest(
        @NotBlank(message = "diagnostic est obligatoire") String diagnostic,
        @NotBlank(message = "traitement est obligatoire") String traitement) {}
