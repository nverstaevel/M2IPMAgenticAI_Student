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
- `@ManyToOne` : « plusieurs créatures ont un propriétaire ». C'est ce côté qui porte la clé étrangère.
- `@OneToMany(mappedBy = "proprietaire")` : le côté « miroir », en lecture.
- Les collections sont chargées **à la demande** (*lazy*) : y accéder hors transaction provoque `LazyInitializationException`.

## Dans la clinique
Une `Creature` référence son `Proprietaire` et son `Espece`. Une `Consultation` référence une créature et un soigneur. Dans `data.sql`, on retrouve les identifiants **par le nom** : `(SELECT id FROM espece WHERE nom = 'Dragon')`, jamais par un numéro écrit en dur.

## À retenir
- Ne jamais écrire un identifiant généré en dur.
- Renvoyer un **DTO** (un `record`), pas l'entité, depuis l'API : c'est le service qui le construit, dans la transaction.

## Erreurs fréquentes
- `LazyInitializationException` : on lit une relation après la fin de la transaction.
- `data.sql` exécuté avant la création des tables : voir la propriété `defer-datasource-initialization`.

## Auto-test
<details class="qa" markdown="1"><summary>Dans « Créature N → 1 Propriétaire », quelle classe porte `@ManyToOne` ?</summary>

`Creature` : c'est elle qui contient la clé étrangère vers le propriétaire.

</details>

<details class="qa" markdown="1"><summary>Pourquoi `(SELECT id FROM espece WHERE nom = 'Dragon')` plutôt que `1` ?</summary>

Les identifiants sont générés : leur valeur dépend de l'ordre d'insertion. Le nom, lui, est stable.

</details>
