---
title: Lire une trace d'agent
resume: Une méthode en cinq questions et un catalogue des échecs.
td: td4, td5, td6
famille: agents
ordre: 3
notions: traces-graphe, arrets-agent, evaluer-agent, texte-tiers-injection
---
## Ouvrir la trace
- Page `/agents` ; après un lancement, notez le `runId` et ouvrez `/agents?run=<runId>`.
- Pour une partie du jeu : un `runId` par journée.

## Cinq questions
1. **Quelle demande** ? Qu'avait-on demandé exactement ?
2. **Quels outils**, dans quel ordre, avec quels arguments ? Rien d'inventé ?
3. **Où part le budget** ? Tokens et durée par étape.
4. **Pourquoi s'est-il arrêté** ? Quel motif ?
5. **La réponse est-elle appuyée** par ce que les outils ont renvoyé ?

## Catalogue des échecs (à enrichir au TD4)
| Échec | Signe dans la trace |
|---|---|
| Boucle | même appel répété |
| Mauvais outil | l'outil choisi ne correspond pas à la question |
| Argument inventé | valeur absente de toute observation précédente |
| Réponse sans preuve | aucun outil appelé alors qu'il en fallait un |
| Abandon prématuré | réponse finale après une erreur d'outil |
| Consigne piégée suivie | appel d'un outil après lecture d'un texte tiers |

Copiez dans `JOURNAL.md` **une** trace commentée par échec observé.
