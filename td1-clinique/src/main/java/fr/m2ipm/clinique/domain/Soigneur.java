package fr.m2ipm.clinique.domain;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Soigneur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nom;

    // TODO TD1 : relation plusieurs-à-plusieurs avec Espece, via la table de jointure « soigneur_espece »
    //           (colonnes soigneur_id et espece_id : voir data.sql et l'étape 2 du sujet)
    private Set<Espece> especesSoignees = new HashSet<>();

    protected Soigneur() {}

    public Soigneur(String nom) { this.nom = nom; }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public Set<Espece> getEspecesSoignees() { return especesSoignees; }
}
