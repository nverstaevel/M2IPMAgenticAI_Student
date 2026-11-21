---
title: Les quatre couches d'une application Spring Boot
resume: Contrôleur, service, dépôt, domaine : chacun son rôle, un seul sens de dépendance.
cm: CM1 — Rappel Spring Boot : Quatre couches, un seul sens de dépendance
td: td1
---
Une application Spring Boot bien rangée se lit de haut en bas :

| Couche | Rôle | Exemple de la clinique |
|---|---|---|
| `controller` | traduit HTTP en appels de service (et inversement) | `ClinicController` |
| `service` | porte les **règles métier** et les transactions | `planifierConsultation` refuse un chevauchement |
| `repository` | accès aux données, une interface Spring Data | `CreatureRepository` |
| `domain` | entités JPA | `Creature`, `Proprietaire` |

**Règle de dépendance** : le contrôleur appelle le service, le service appelle le dépôt, jamais l'inverse. Un contrôleur qui contient une règle métier, ou un service qui connaît HTTP, est un signe d'alerte.

**Pourquoi ça compte pour la suite** : au TD2, les *outils* du modèle appellent les **services** (jamais les dépôts ni les contrôleurs). Si la règle de chevauchement est dans le service, elle protège aussi bien l'API REST que l'agent.

**Vérifiez que vous avez compris** : où mettriez-vous la vérification « un soigneur ne soigne pas toutes les espèces » ?
