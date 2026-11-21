---
title: Port et adaptateur
resume: Cacher une dépendance externe derrière une interface, pour pouvoir la remplacer par une version simulée.
cm: CM2 — Des ports pour isoler les sources ; Un port pour isoler l’agenda
td: td3
---
Un **port** est une interface de votre domaine (`AgendaPort`). Un **adaptateur** est une implémentation qui parle à l'extérieur (`GoogleAgendaAdapter`) ; un autre peut être simulé (`AgendaSimule`).

- Le reste du code ne connaît que le port.
- Le choix se fait par **profil** : `@Profile("!simule")` pour le réel, `@Profile("simule")` pour le simulé.
- Bénéfice : les tests et l'évaluation tournent **sans réseau, sans compte, sans clé**, et de façon reproductible.

Le même raisonnement s'applique à `WikipediaClient` : on lui passe un `RestClient` préconfiguré dans les tests.
