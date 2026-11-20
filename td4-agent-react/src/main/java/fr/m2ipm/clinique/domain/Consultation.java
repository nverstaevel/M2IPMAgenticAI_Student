package fr.m2ipm.clinique.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Consultation {
    /** Durée fixe d'une consultation, en minutes. */
    public static final int DUREE_MINUTES = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private LocalDateTime dateHeure;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Creature creature;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Soigneur soigneur;

    private String motif;
    private String diagnostic;
    private String traitement;
    private BigDecimal tarif;

    protected Consultation() {}

    public Consultation(LocalDateTime dateHeure, Creature creature, Soigneur soigneur, String motif, BigDecimal tarif) {
        this.dateHeure = dateHeure;
        this.creature = creature;
        this.soigneur = soigneur;
        this.motif = motif;
        this.tarif = tarif;
    }

    public void cloturer(String diagnostic, String traitement) {
        this.diagnostic = diagnostic;
        this.traitement = traitement;
    }

    public Long getId() { return id; }
    public LocalDateTime getDateHeure() { return dateHeure; }
    public Creature getCreature() { return creature; }
    public Soigneur getSoigneur() { return soigneur; }
    public String getMotif() { return motif; }
    public String getDiagnostic() { return diagnostic; }
    public String getTraitement() { return traitement; }
    public BigDecimal getTarif() { return tarif; }
}
