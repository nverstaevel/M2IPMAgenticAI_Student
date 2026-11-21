---
title: ChatClient et prompt système
resume: Le ChatClient est l'API fluide de Spring AI ; le prompt système fixe le rôle.
cm: CM1 — Deux niveaux d’abstraction ; ChatClient en quelques lignes
td: td2
---
Spring AI offre deux niveaux : `ChatModel` (bas niveau : un `Prompt` en entrée, une `ChatResponse` en sortie) et `ChatClient` (haut niveau, fluide : prompt système par défaut, conseillers, outils, sortie structurée).

Un **prompt système** décrit : le rôle, la langue et le ton, **quand utiliser les outils**, et ce que le modèle ne doit **jamais** inventer. Testez-le comme du code : une grille de questions, plusieurs variantes, plusieurs exécutions.

Un `ChatClient` se construit **une fois** (bean) avec ses valeurs par défaut (`defaultSystem`, `defaultAdvisors`, `defaultTools`) puis s'utilise par appel (`prompt().user(…).call().content()`). Pour des usages différents (chat avec mémoire, triage isolé), construisez **deux clients**.
