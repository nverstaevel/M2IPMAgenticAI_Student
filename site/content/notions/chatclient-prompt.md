---
title: ChatClient et prompt système
resume: Le ChatClient est l'API fluide de Spring AI ; le prompt système fixe le rôle.
cm: CM1 — Deux niveaux d’abstraction ; ChatClient en quelques lignes
td: td2
---
## L'idée
Le `ChatClient` est l'objet qui parle au modèle. Le **prompt système** lui donne son rôle ; les conseillers (*advisors*) et les outils lui donnent des capacités.

## Comprendre
{{fig:chatclient}}

Deux niveaux d'abstraction : `ChatModel` (bas niveau : un `Prompt` en entrée, une `ChatResponse` en sortie) et `ChatClient` (fluide, avec valeurs par défaut).

Un bon **prompt système** dit :
1. le **rôle** et le ton (« Tu es le Dr Gribouille, vétérinaire facétieux ») ;
2. la **langue** ;
3. **quand utiliser les outils** (« pour toute question sur une créature, utilise l'outil, ne devine pas ») ;
4. ce qu'il ne doit **jamais** faire ou inventer (« si tu ne sais pas, dis-le »).

On construit **un** `ChatClient` par usage : un pour le chat (mémoire, outils), un autre pour le triage (sans mémoire, sortie structurée).

## À retenir
- Se construit une fois (bean), s'utilise à chaque appel.
- Le prompt système se **teste** comme du code : grille de questions, plusieurs variantes, plusieurs exécutions.

## Erreurs fréquentes
- Un prompt système trop long : il est relu à chaque appel (coût) et se dilue.
- Un seul `ChatClient` pour tout : mémoire et outils parasitent un appel simple.

## Auto-test
Q: Pourquoi deux `ChatClient` plutôt qu'un seul ?
R: Les usages diffèrent : le chat a mémoire et outils, le triage doit être isolé et déterministe. Les mélanger dégrade les deux.
