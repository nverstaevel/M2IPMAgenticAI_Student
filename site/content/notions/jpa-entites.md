---
title: Entités JPA et relations
resume: Une classe annotée correspond à une table ; les relations se déclarent côté propriétaire de la clé étrangère.
cm: CM1 — Rappel Spring Boot : Une entité JPA correspond à une table
td: td1
famille: spring
ordre: 2
---
## L'idée
Une **entité** est une classe Java dont chaque instance correspond à une ligne de table. JPA fait la traduction objet ⇄ relationnel à votre place.

## Comprendre
{{fig:jpa}}

- `@Entity` + `@Id` + `@GeneratedValue` : l'identifiant est **généré par la base**.
- Une **association** se déclare avec l'une des quatre annotations ci-dessous. Dans chaque cas, un seul côté **porte** la clé étrangère (le côté propriétaire) ; l'autre, facultatif, est un miroir déclaré avec `mappedBy`.
- Les collections sont chargées **à la demande** (*lazy*) : y accéder hors transaction provoque `LazyInitializationException`.

## Les associations, une par une
Exemples **hors clinique** (une bibliothèque : livres, auteurs, genres, cartes de lecteur). À vous de transposer dans la clinique.

| Annotation | Se lit | Qui porte la clé étrangère | Où elle vit |
|---|---|---|---|
| `@ManyToOne` | plusieurs livres → un auteur | le côté « plusieurs » (`Livre`) | colonne `auteur_id` dans `livre` |
| `@OneToMany(mappedBy)` | un auteur → plusieurs livres | personne : miroir de la précédente | aucune colonne |
| `@ManyToMany` | des livres ↔ des genres | aucun des deux : une **table de jointure** | table `livre_genre` (deux colonnes) |
| `@OneToOne` | un lecteur ↔ une carte | le côté qui déclare `@JoinColumn` | colonne `carte_id` (unique) |

**1. `@ManyToOne` et son miroir `@OneToMany`** : plusieurs livres ont un même auteur.

```java
@Entity
class Livre {
    @Id @GeneratedValue Long id;
    String titre;

    @ManyToOne(optional = false)          // clé étrangère auteur_id dans la table livre
    Auteur auteur;
}

@Entity
class Auteur {
    @Id @GeneratedValue Long id;
    String nom;

    @OneToMany(mappedBy = "auteur")       // miroir : pas de colonne, on lit seulement
    List<Livre> livres = new ArrayList<>();
}
```

**2. `@ManyToMany`** : un livre a plusieurs genres, un genre regroupe plusieurs livres. Aucune des deux tables ne peut porter la clé : il faut une table de jointure, que l'on décrit avec `@JoinTable` sur **un** des deux côtés.

```java
@Entity
class Livre {
    // ...
    @ManyToMany
    @JoinTable(name = "livre_genre",
        joinColumns = @JoinColumn(name = "livre_id"),           // pointe vers cette entité
        inverseJoinColumns = @JoinColumn(name = "genre_id"))    // pointe vers l'autre
    Set<Genre> genres = new HashSet<>();
}

@Entity
class Genre {
    @Id @GeneratedValue Long id;
    String nom;

    @ManyToMany(mappedBy = "genres")      // miroir, sans @JoinTable
    Set<Livre> livres = new HashSet<>();
}
```

**3. `@OneToOne`** : un lecteur a une seule carte, une carte appartient à un seul lecteur. C'est un `@ManyToOne` dont la clé est déclarée **unique**.

```java
@Entity
class Lecteur {
    @Id @GeneratedValue Long id;
    String nom;

    @OneToOne
    @JoinColumn(name = "carte_id", unique = true)   // clé étrangère, une carte par lecteur
    Carte carte;
}

@Entity
class Carte {
    @Id @GeneratedValue Long id;
    String numero;

    @OneToOne(mappedBy = "carte")         // miroir facultatif
    Lecteur lecteur;
}
```

Pour choisir, posez deux questions : « combien d'objets de l'autre côté pour **un** objet de mon côté ? », puis la même question dans l'autre sens. 1 et plusieurs → `@ManyToOne` ; plusieurs et plusieurs → `@ManyToMany` ; 1 et 1 → `@OneToOne`.

## Dans la clinique
Une `Creature` référence son `Proprietaire` et son `Espece`. Une `Consultation` référence une créature et un soigneur. Quelles associations reconnaissez-vous ? Une créature a **un** propriétaire et **une** espèce, mais un propriétaire possède **plusieurs** créatures. Un soigneur soigne **plusieurs** espèces, et une espèce a **plusieurs** soigneurs : la table `soigneur_espece` de `data.sql` est une table de jointure. Dans `data.sql`, on retrouve les identifiants **par le nom** : `(SELECT id FROM espece WHERE nom = 'Dragon')`, jamais par un numéro écrit en dur.

## À retenir
- Ne jamais écrire un identifiant généré en dur.
- Renvoyer un **DTO** (un `record`), pas l'entité, depuis l'API : c'est le service qui le construit, dans la transaction.

## Erreurs fréquentes
- Déclarer `@JoinTable` des **deux** côtés d'un `@ManyToMany`, ou oublier `mappedBy` côté miroir : JPA crée deux tables de jointure.
- Mettre `@ManyToOne` du mauvais côté : la clé étrangère va dans la table de l'autre entité.
- `LazyInitializationException` : on lit une relation après la fin de la transaction.
- `data.sql` exécuté avant la création des tables : voir la propriété `defer-datasource-initialization`.

## Auto-test
<details class="qa" markdown="1"><summary>Dans « Créature N → 1 Propriétaire », quelle classe porte `@ManyToOne` ?</summary>

`Creature` : c'est elle qui contient la clé étrangère vers le propriétaire.

</details>

<details class="qa" markdown="1"><summary>Pourquoi `(SELECT id FROM espece WHERE nom = 'Dragon')` plutôt que `1` ?</summary>

Les identifiants sont générés : leur valeur dépend de l'ordre d'insertion. Le nom, lui, est stable.

</details>

<details class="qa" markdown="1"><summary>Un soigneur soigne plusieurs espèces et une espèce a plusieurs soigneurs : quelle annotation, et où est la clé étrangère ?</summary>

`@ManyToMany`. Il n'y a pas de clé étrangère dans `soigneur` ni dans `espece` : elles sont dans la table de jointure (`soigneur_espece`), déclarée avec `@JoinTable` sur un seul des deux côtés.

</details>

<details class="qa" markdown="1"><summary>Quelle différence entre `@ManyToOne` et `@OneToOne` côté base de données ?</summary>

Aucune colonne en plus : dans les deux cas, une clé étrangère. Avec `@OneToOne`, cette colonne est déclarée **unique** : deux lignes ne peuvent pas pointer vers la même cible.

</details>
