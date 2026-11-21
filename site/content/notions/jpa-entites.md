---
title: Entités JPA et relations
resume: Une classe annotée correspond à une table ; les relations se déclarent côté propriétaire de la clé étrangère.
cm: CM1 — Rappel Spring Boot : Une entité JPA correspond à une table
td: td1
---
Une **entité** est une classe `@Entity` dont chaque instance est une ligne. Les points à retenir :

- `@Id` + `@GeneratedValue` : l'identifiant est généré par la base ; ne l'écrivez jamais en dur dans `data.sql`.
- `@ManyToOne` (une créature a un propriétaire) porte la clé étrangère ; `@OneToMany(mappedBy = …)` est le côté « miroir ».
- Les relations sont **paresseuses** par défaut dans les collections : y accéder hors transaction provoque `LazyInitializationException`.

**Piège classique** : exposer l'entité directement dans l'API. Renvoyez plutôt un **DTO** (un `record`) construit dans le service, dans la transaction.

Dans `data.sql`, retrouvez les identifiants par le nom : `(SELECT id FROM espece WHERE nom = 'Dragon')`.
