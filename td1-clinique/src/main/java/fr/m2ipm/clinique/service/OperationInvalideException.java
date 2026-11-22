package fr.m2ipm.clinique.service;

/** Opération refusée par une règle de gestion (400). */
public class OperationInvalideException extends RuntimeException {
    public OperationInvalideException(String message) { super(message); }
}
