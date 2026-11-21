---
title: Boucle bornée (garde-fous d'agent)
aka: Circuit breaker, budget, guardrails
famille: Enchaînements et boucles
niveau: 1
cout: réduit le coût
controle: le code impose des limites
resume: Plafond d'étapes, budget de tokens, détection de boucle, gestion d'erreur : l'agent s'arrête toujours.
probleme: Éviter qu'un agent ne tourne sans fin ou sans plafond de coût.
td: td4
notions: arrets-agent, boucle-react, tester-sans-modele
recettes: borner-un-agent, tester-avec-modele-simule
---
## Intention
Garantir qu'**aucune exécution ne dépasse** ni un nombre d'étapes, ni un budget, ni ne répète indéfiniment le même appel.

## Problème
Une boucle pilotée par un modèle n'a aucune raison de s'arrêter. Une erreur de raisonnement devient une facture.

## Structure
{{fig:arrets}}

## Participants
| Participant | Rôle |
|---|---|
| Compteur d'étapes | `PLAFOND_ETAPES` |
| Compteur de tokens | `BUDGET_TOKENS` |
| Signature d'appel | `BOUCLE_DETECTEE` |
| `try/catch` | `ERREUR` |

## Dynamique
Après chaque étape, vérifier les seuils **dans l'ordre** ; au premier dépassé, arrêter et **inscrire le motif** dans la trace.

## Conséquences
- Coût **borné**, comportement **explicable**.
- Chaque arrêt se **teste** avec le modèle simulé.
{: .plus }

- Un plafond trop bas coupe un raisonnement légitime.
{: .moins }

## Quand l'utiliser
Pour **tout** agent.

## Dans la clinique
TD4, étape 2 : trois arrêts à écrire (plafond, boucle, erreur), deux fournis (budget, réponse finale).

## Mesurer
Répartition des motifs d'arrêt sur vos essais : trop de `PLAFOND_ETAPES` signale un problème d'outil ou de prompt.
