package fr.m2ipm.jeu;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Détient la partie en cours (une seule à la fois). Le redémarrage n'est pas un outil MCP : un agent ne doit pas pouvoir rejouer. */
@Service
public class PartieService {

    private final long graine;
    private final int jours;
    private final int arrivees;
    private volatile Jeu jeu;

    public PartieService(@Value("${jeu.graine:42}") long graine, @Value("${jeu.jours:5}") int jours,
                         @Value("${jeu.arrivees-par-jour:3}") int arrivees) {
        this.graine = graine;
        this.jours = jours;
        this.arrivees = arrivees;
        this.jeu = new Jeu(graine, jours, arrivees);
    }

    public Jeu jeu() { return jeu; }

    /** Nouvelle partie, avec la graine donnée (la même graine donne les mêmes patients). */
    public Jeu nouvelle(Long graineDemandee) {
        jeu = new Jeu(graineDemandee == null ? graine : graineDemandee, jours, arrivees);
        return jeu;
    }
}
