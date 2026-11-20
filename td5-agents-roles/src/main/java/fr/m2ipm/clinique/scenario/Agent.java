package fr.m2ipm.clinique.scenario;

/**
 * Un agent exécutable par l'orchestrateur : déclarez-le comme bean Spring (@Component("nom") ou @Bean("nom")),
 * puis référencez ce nom dans un fichier de scénario. Tout le reste (ordre des appels, boucles, graphe, trace, tokens) est fourni.
 */
@FunctionalInterface
public interface Agent {

    Resultat executer(Entree entree);
}
