package fr.m2ipm.clinique.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Proprietaire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nom;
    private String prenom;
    private String ville;
    private String telephone;

    @OneToMany(mappedBy = "proprietaire")
    private List<Creature> creatures = new ArrayList<>();

    protected Proprietaire() {}

    public Proprietaire(String nom, String prenom, String ville, String telephone) {
        this.nom = nom;
        this.prenom = prenom;
        this.ville = ville;
        this.telephone = telephone;
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getVille() { return ville; }
    public String getTelephone() { return telephone; }
    public List<Creature> getCreatures() { return creatures; }
}
