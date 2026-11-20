package fr.m2ipm.clinique.scenario;

/** Un scénario ne peut pas être exécuté (définition invalide, agent absent, erreur d'un agent). */
public class ScenarioException extends RuntimeException {

    public ScenarioException(String message) { super(message); }

    public ScenarioException(String message, Throwable cause) { super(message, cause); }
}
