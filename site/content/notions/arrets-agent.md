---
title: Les arrêts d'un agent
resume: Rien ne garantit qu'un agent s'arrête : c'est à vous de borner la boucle.
cm: CM2 — Cinq motifs d’arrêt
td: td4
---
## L'idée
Rien ne garantit qu'un agent s'arrête : c'est à vous de **borner la boucle**. Chaque arrêt est un signal observable, comparé à un seuil.

## Comprendre
{{fig:arrets}}

| Motif d'arrêt | Seuil | Se teste avec le modèle simulé par… |
|---|---|---|
| `REPONSE_FINALE` | plus d'outil demandé | une réponse sans `outil:` |
| `PLAFOND_ETAPES` | `numero >= maxEtapes` | un modèle qui demande toujours un outil |
| `BUDGET_TOKENS` | cumul ≥ budget | des réponses annonçant beaucoup de tokens |
| `BOUCLE_DETECTEE` | même appel que l'étape précédente | le même `outil:NOM:{json}` répété |
| `ERREUR` | exception du modèle ou d'un outil | un outil qui lève |

Un agent sans arrêt, c'est une facture sans plafond.

## À retenir
- Un motif d'arrêt = un seuil + un test.
- L'**ordre** des vérifications compte (voir le schéma).

## Erreurs fréquentes
- Détecter la boucle sur le nom d'outil seul : deux appels du même outil avec des arguments différents sont légitimes. Comparez **outil + arguments**.

## Auto-test
Q: Pourquoi comparer aussi les arguments pour détecter une boucle ?
R: Parce qu'appeler deux fois le même outil avec des arguments différents est normal ; seul le même appel répété est suspect.
