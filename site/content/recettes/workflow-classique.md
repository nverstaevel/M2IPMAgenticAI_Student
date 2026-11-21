---
title: Écrire le workflow de référence (sans modèle décideur)
resume: Le même traitement enchaîné par du code Java : la référence à laquelle comparer un agent.
td: td5
famille: agents
ordre: 7
notions: workflow-ou-agent, evaluer-agent, boucle-react, agents-roles
---
## Objectif
Obtenir une **référence** : le même résultat, mais avec un enchaînement fixé par le code.

{{fig:workflow}}

## Les étapes
1. Listez les étapes **toujours identiques** du traitement (lire le dossier → proposer → vérifier → présenter).
2. Écrivez-les en Java, dans l'ordre. Le modèle n'est appelé que pour **remplir** chaque étape (via `.entity(...)`).
3. Gardez les mêmes entrées, la même sortie et les mêmes outils que l'architecture à comparer.
4. Mesurez comme pour l'agent : tokens, durée, exactitude.

## Lire le résultat
| | Workflow | Agents |
|---|---|---|
| Tokens | généralement plus bas | plus élevés |
| Variabilité | faible | élevée |
| Cas inattendus | mal gérés | mieux gérés |

**La question n'est pas « qui gagne ? » mais « pour quel type de cas l'autonomie rapporte-t-elle ? »**

## Pièges
- Comparer avec des outils différents : on ne sait plus ce qu'on mesure.
- Conclure sur un seul cas.
