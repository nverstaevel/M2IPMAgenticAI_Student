---
title: Port et adaptateur
resume: Cacher une dépendance externe derrière une interface, pour pouvoir la remplacer par une version simulée.
cm: CM2 — Des ports pour isoler les sources ; Un port pour isoler l’agenda
td: td3
---
## L'idée
Cacher une dépendance externe derrière une **interface** (le port), pour pouvoir la remplacer par une version simulée.

## Comprendre
{{fig:port_adaptateur}}

- Le **port** (`AgendaPort`) est dans votre domaine : il parle votre langage (`creneauxLibres`, `creerEvenement`).
- Un **adaptateur** l'implémente : `GoogleAgendaAdapter` pour le réel, `AgendaSimule` pour les tests.
- Le choix se fait par **configuration** : une propriété (`agenda.mode=simule` ou `google`) lue par une classe `@Configuration` qui crée un seul des deux adaptateurs (`@ConditionalOnProperty`).

Bénéfice : tests et évaluation tournent **sans réseau, sans compte, sans clé**, et de façon reproductible.

## Dans la clinique
Même idée pour `WikipediaClient` : en test, on lui passe un `RestClient` préconfiguré qui répond avec des JSON d'exemple.

## À retenir
- Le reste du code ne connaît que le port.
- Un seul adaptateur actif à la fois.

## Erreurs fréquentes
- Créer les deux adaptateurs en même temps : deux beans pour un seul port, erreur au démarrage. Une condition (`@ConditionalOnProperty`) n'en laisse vivre qu'un.
- Faire fuir des types de l'API Google dans le port.

## Auto-test
Q: Pourquoi `AgendaSimule` est-il nécessaire à l'évaluation de TD ?
R: Parce que l'évaluation tourne sans réseau ni compte : l'adaptateur réel serait inutilisable.
