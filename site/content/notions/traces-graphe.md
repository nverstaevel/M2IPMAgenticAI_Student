---
title: Traces et graphe d'exécution
resume: Lire ce que l'agent a réellement fait : étapes, outils, tokens, durée.
cm: CM2 — Une ligne par étape, pour comprendre ; CM2 — Lire le graphe d’exécution d’un agent
td: td4, td5, td6
---
## L'idée
Une **trace** est ce que l'agent a réellement fait : étapes, outils, arguments, tokens, durée. C'est votre document d'enquête.

## Comprendre
La page `/agents` dessine le **graphe réel** d'une exécution (pointillés = prévu, traits pleins = réel). Lisez-le en **cinq questions** :

1. Quelle **demande** ?
2. Quels **outils**, dans quel ordre, avec quels arguments ? Rien d'inventé ?
3. Où part le **budget** (tokens et durée par étape) ?
4. Pourquoi s'est-il **arrêté** ?
5. La réponse est-elle **appuyée** par ce que les outils ont renvoyé ?

| Échec | Signe dans la trace |
|---|---|
| Boucle | même appel répété |
| Mauvais outil | l'outil ne correspond pas à la question |
| Argument inventé | valeur absente de toute observation |
| Réponse sans preuve | aucun outil alors qu'il en fallait un |
| Consigne piégée suivie | appel inattendu après lecture d'un texte tiers |

## À retenir
- On comprend un agent en **lisant**, pas en devinant.
- Notez au moins **une trace commentée par échec** dans `JOURNAL.md`.

## Auto-test
Q: Un agent répond juste sans avoir appelé aucun outil. Bon signe ?
R: Pas forcément : il a peut-être deviné. Vérifiez qu'un outil était nécessaire ; sinon c'est une « réponse sans preuve ».
