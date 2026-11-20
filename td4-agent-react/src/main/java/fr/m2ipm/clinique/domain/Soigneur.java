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

    @ManyToMany
    @JoinTable(name = "soigneur_espece",
            joinColumns = @JoinColumn(name = "soigneur_id"),
            inverseJoinColumns = @JoinColumn(name = "espece_id"))
    private Set<Espece> especesSoignees = new HashSet<>();

    protected Soigneur() {}

    public Soigneur(String nom) { this.nom = nom; }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public Set<Espece> getEspecesSoignees() { return especesSoignees; }
}
