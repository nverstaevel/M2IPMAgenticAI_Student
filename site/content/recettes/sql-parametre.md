---
title: Interroger une base sans jamais laisser le modèle écrire du SQL
resume: Requêtes paramétrées dans votre code, deuxième source de données avec son `@Qualifier`.
td: td3
notions: appel-outils, texte-tiers-injection, couches-spring
---
## Principe
Le modèle fournit des **valeurs** (nom, espèce). Votre code contient le **SQL**, avec des paramètres nommés.

```java
return jdbc.sql("""
        SELECT m.nom, m.principe_actif, e.dose
        FROM medicament m JOIN medicament_espece e ON e.medicament_id = m.id
        WHERE LOWER(m.nom) LIKE LOWER(:nom) AND LOWER(e.espece) = LOWER(:espece)
        """)
    .param("nom", "%" + nom.trim() + "%")
    .param("espece", espece.trim())
    .query((rs, i) -> new Fiche(rs.getString("nom"), rs.getString("principe_actif"), rs.getString("dose")))
    .list();
```

## Deux bases dans une application
La seconde source de données (`CatalogueConfig`) est fournie. On l'injecte par son nom : `@Qualifier("catalogue") JdbcClient jdbc`.

## Ce qui doit marcher
- `chercherMedicament("baume", "Dragon")` → fiche avec les contre-indications ;
- `chercherMedicament("'; DROP TABLE medicament; --", "Dragon")` → « Aucun médicament » (le paramètre est une valeur, pas du SQL) ;
- jamais d'outil du type `executerSql(String requete)`.
