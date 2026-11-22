package fr.m2ipm.clinique.dto;

import java.util.List;

public record SoigneurDto(Long id, String nom, List<String> especes) {}
