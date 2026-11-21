---
title: Workflow ou agent : qui décide de la suite ?
resume: Un workflow enchaîne selon votre code ; un agent laisse le modèle choisir.
cm: CM2 — Penser, agir, observer ; CM3 — Comparer deux architectures
td: td4, td5
---
## L'idée
Deux manières d'enchaîner des appels au modèle : un **workflow** (le code décide de l'enchaînement) ou un **agent** (le modèle décide de la suite).

## Comprendre
{{fig:spectre}}

| | Workflow | Agent |
|---|---|---|
| Qui décide de la suite ? | votre code Java | le modèle |
| Prévisible ? | oui | non |
| Testable ? | facilement | par traces et mesures |
| Coût | faible, borné | variable |
| Quand | le déroulé est connu | le déroulé dépend de ce qu'on découvre |

{{fig:workflow}}

**Règle d'ingénieur** : prenez l'option la plus à gauche qui résout le problème. Un agent n'est pas « mieux » : il est plus **flexible** et plus **risqué**.

## À retenir
- Le workflow classique est la **référence** à laquelle on compare un agent (TD5).
- Autonomie ≠ qualité.

## Erreurs fréquentes
- Choisir un agent « parce que c'est moderne » : si le déroulé est connu, un workflow est moins cher et plus prévisible.
- Comparer sur un seul essai : un agent varie, il faut plusieurs exécutions.
- Oublier le coût de la maintenance : ajouter un cas à un workflow, c'est écrire du code ; à un agent, c'est parfois modifier un prompt.

## Auto-test
Q: Le traitement d'une consultation suit toujours les mêmes quatre étapes. Workflow ou agent ?
R: Workflow : le déroulé est connu, donc le code peut l'imposer, pour moins cher et plus sûr.
