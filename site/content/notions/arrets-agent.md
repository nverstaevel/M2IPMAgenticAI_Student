---
title: Les arrêts d'un agent
resume: Rien ne garantit qu'un agent s'arrête : c'est à vous de borner la boucle.
cm: CM2 — Cinq motifs d’arrêt
td: td4
---
| Motif d'arrêt | Quand |
|---|---|
| `REPONSE_FINALE` | le modèle ne demande plus d'outil |
| `PLAFOND_ETAPES` | trop d'étapes (`numero >= maxEtapes`) |
| `BUDGET_TOKENS` | trop de tokens cumulés |
| `BOUCLE_DETECTEE` | même outil, mêmes arguments que l'étape précédente |
| `ERREUR` | exception du modèle ou d'un outil |

Chaque arrêt est un **signal observable** à comparer à une valeur seuil, et se **teste** avec le modèle simulé (`outil:NOM:{json}` déclenche l'appel de l'outil NOM). Un agent sans arrêt, c'est une facture sans plafond.
