package fr.m2ipm.clinique.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Creature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nom;
    private LocalDate dateNaissance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Espece espece;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Proprietaire proprietaire;

    @OneToMany(mappedBy = "creature")
    private List<Consultation> consultations = new ArrayList<>();

    protected Creature() {}

    public Creature(String nom, LocalDate dateNaissance, Espece espece, Proprietaire proprietaire) {
        this.nom = nom;
        this.dateNaissance = dateNaissance;
        this.espece = espece;
        this.proprietaire = proprietaire;
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public LocalDate getDateNaissance() { return dateNaissance; }
    public Espece getEspece() { return espece; }
    public Proprietaire getProprietaire() { return proprietaire; }
    public List<Consultation> getConsultations() { return consultations; }
}
