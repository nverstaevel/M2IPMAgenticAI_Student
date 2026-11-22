package fr.m2ipm.clinique.service;

/** Ressource inexistante (404). Le message est écrit pour être lu par un humain et par un modèle. */
public class RessourceIntrouvableException extends RuntimeException {
    public RessourceIntrouvableException(String message) { super(message); }
}
