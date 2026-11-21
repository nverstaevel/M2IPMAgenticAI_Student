---
title: Déclarer une entité et son jeu de données
resume: Entités JPA, relations, et data.sql qui retrouve les identifiants par le nom.
td: td1
notions: jpa-entites, spring-data-services, couches-spring
---
## Objectif
Ajouter une entité liée aux autres et l'alimenter en données de test sans jamais écrire d'identifiant en dur.

## Squelette (exemple hors clinique)
```java
@Entity
class Emprunt {
    @Id @GeneratedValue Long id;
    @ManyToOne(optional = false) Livre livre;
    @ManyToOne(optional = false) Membre membre;
    LocalDate dateEmprunt;
}
```
```sql
-- data.sql : on retrouve les identifiants par le nom
INSERT INTO emprunt (livre_id, membre_id, date_emprunt)
VALUES ((SELECT id FROM livre WHERE titre = 'Dune'),
        (SELECT id FROM membre WHERE nom = 'Alice'), '2026-04-02');
```

## Vérifier
- l'application démarre : les tables sont créées **avant** `data.sql` (`spring.jpa.defer-datasource-initialization=true`) ;
- un test lit la ligne et son lien (`emprunt.getLivre().getTitre()`).

## Pièges
- Apostrophe dans une valeur SQL : doublez-la (`'Mue d''hiver'`).
- Accéder à une relation hors transaction : `LazyInitializationException`.
