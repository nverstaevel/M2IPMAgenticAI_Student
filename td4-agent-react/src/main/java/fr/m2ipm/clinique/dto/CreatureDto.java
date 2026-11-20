package fr.m2ipm.clinique.dto;

import java.time.LocalDate;

public record CreatureDto(Long id, String nom, LocalDate dateNaissance, String espece, Long proprietaireId) {}
