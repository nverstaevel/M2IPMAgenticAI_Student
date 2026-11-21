---
title: Borner une boucle d'agent (plafond, budget, boucle)
resume: Les cinq motifs d'arrêt : où les tester dans la boucle et comment les prouver avec le modèle simulé.
td: td4
notions: arrets-agent, boucle-react, tester-sans-modele, traces-graphe
---
## Objectif
Qu'aucune exécution ne puisse tourner sans fin ni sans plafond de coût.

{{fig:arrets}}

## Les étapes
1. Gardez **un compteur d'étapes** ; dès que `numero >= maxEtapes` : `PLAFOND_ETAPES`.
2. Cumulez les **tokens** de chaque étape ; au-delà du budget : `BUDGET_TOKENS` (fourni, à lire).
3. Calculez la **signature** d'un appel (nom d'outil + arguments sérialisés) ; si elle égale celle de l'étape précédente : `BOUCLE_DETECTEE`.
4. Entourez l'appel du modèle et des outils d'un `try/catch` : `ERREUR`.
5. Si aucun outil n'est demandé : `REPONSE_FINALE`.
6. **Journalisez** le motif d'arrêt dans la trace.

## Un test par motif
| Motif | Modèle simulé |
|---|---|
| `PLAFOND_ETAPES` | demande toujours un outil, avec des arguments différents |
| `BOUCLE_DETECTEE` | répète exactement `outil:consulterTarif:{…}` |
| `ERREUR` | appelle un outil qui lève |

## Pièges
- Comparer seulement le nom de l'outil : deux appels légitimes seraient pris pour une boucle.
- Oublier d'inscrire le motif dans la trace : on ne comprend plus l'arrêt.
