package fr.m2ipm.clinique.service;

/** Le soigneur a déjà une consultation qui chevauche le créneau demandé (409). */
public class ConflitDeCreneauException extends RuntimeException {
    public ConflitDeCreneauException(String message) { super(message); }
}
