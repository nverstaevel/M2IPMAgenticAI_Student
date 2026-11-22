package fr.m2ipm.clinique.domain;

import jakarta.persistence.*;

@Entity
public class Espece {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String nom;
    private String habitat;

    protected Espece() {}

    public Espece(String nom, String habitat) {
        this.nom = nom;
        this.habitat = habitat;
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public String getHabitat() { return habitat; }
}
