---
title: Comparer deux architectures d'agents
resume: Mêmes conditions, plusieurs parties, mesures puis traces : une conclusion honnête.
td: td5, td6
famille: agents
ordre: 9
notions: evaluer-agent, traces-graphe, workflow-ou-agent, moindre-privilege
---
## Objectif
Dire, avec des éléments, laquelle de vos deux architectures fait mieux au jeu — et **pourquoi**.

{{fig:evaluation}}

## Les étapes
1. **Hypothèse écrite** avant de lancer : « B sépare lecture et écriture, donc il tombera moins dans les pièges ».
2. **Conditions fixes** : même graine du jeu, même modèle, même nombre de jours.
3. **Plusieurs parties** par architecture (au moins 3) ; notez le score de chacune.
4. **Mesures** : score, tokens, nombre d'étapes, motifs d'arrêt.
5. **Traces** : ouvrez la meilleure et la pire partie de chaque architecture ; cherchez les causes.
6. **Conclusion** : ce qui est établi, ce qui reste incertain, ce que vous testeriez ensuite.

## Tableau type (chiffres fictifs, pour la forme)
| Architecture | Parties | Score moyen | Écart | Tokens moyens | Pièges évités |
|---|---|---|---|---|---|
| A (un agent) | 3 | 118 | ±15 | 62 000 | 1/3 |
| B (trois rôles) | 3 | 131 | ±6 | 88 000 | 3/3 |

## Pièges
- Une seule partie.
- Changer l'architecture **et** le modèle entre deux essais.
