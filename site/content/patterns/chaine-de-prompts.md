---
title: Chaîne de prompts (workflow classique)
aka: Prompt chaining, workflow, pipeline
famille: Enchaînements et boucles
niveau: 1
cout: faible, borné
controle: le code décide de tout l'enchaînement
resume: Le code Java fixe les étapes ; le modèle ne fait que remplir chacune.
probleme: Un déroulé connu à l'avance, à faire de façon fiable et peu coûteuse.
td: td5
notions: workflow-ou-agent, sortie-structuree, evaluer-agent
recettes: workflow-classique, comparer-deux-architectures
---
## Intention
Obtenir un traitement **prévisible** en imposant l'enchaînement par le code.

## Problème
Quand les étapes sont toujours les mêmes, laisser le modèle « décider » ajoute du coût, de la variabilité et du risque sans bénéfice.

## Structure
{{fig:workflow}}

## Participants
| Participant | Rôle |
|---|---|
| Code d'orchestration | enchaîne les étapes, vérifie les sorties |
| Appels au modèle | un par étape, formats structurés |
| Outils | appelés par le code quand il le décide |

## Dynamique
1. Étape 1 : résumer (modèle) → objet.
2. Étape 2 : classer (modèle) → objet.
3. Étape 3 : rédiger (modèle) → texte.
4. Le code **contrôle** entre deux étapes (garde-fous, validation).

## Conséquences
- Prévisible, testable, **moins cher**.
- Chaque étape est évaluable séparément.
{: .plus }

- Rigide : un cas imprévu n'est pas géré.
- Il faut connaître le déroulé à l'avance.
{: .moins }

## Quand l'utiliser
Déroulé **connu** et stable. C'est la **référence** par défaut.

## Quand l'éviter
Quand la suite dépend de ce qu'on découvre en chemin.

## Dans la clinique
TD5 : `TraiterConsultationWorkflow`, l'enchaînement fixe auquel on compare le duo vétérinaire / gardien.

## Mesurer
Tokens, durée, exactitude — comme pour un agent, sur les mêmes cas.
