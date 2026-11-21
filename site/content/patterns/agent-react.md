---
title: Agent ReAct
aka: Reason + Act, agent à boucle, tool-use loop
famille: Enchaînements et boucles
niveau: 2
cout: moyen à élevé, variable
controle: le modèle décide de la suite
resume: Une boucle où le modèle raisonne, appelle un outil, observe, et recommence jusqu'à pouvoir répondre.
probleme: Un déroulé qui dépend de ce que l'on découvre en route.
td: td4
notions: boucle-react, appel-outils, arrets-agent, traces-graphe
recettes: boucle-react, borner-un-agent, lire-une-trace
---
## Intention
Laisser le modèle **choisir** quels outils appeler, dans quel ordre, et quand s'arrêter.

## Problème
Le nombre et l'ordre des appels dépendent de la question et des résultats intermédiaires : un workflow fixe ne peut pas le prévoir.

## Structure
{{fig:react}}

## Participants
| Participant | Rôle |
|---|---|
| Modèle | pense, choisit un outil, décide de conclure |
| Boucle d'agent (votre code) | enchaîne les étapes, journalise |
| `ToolCallingManager` | exécute les outils demandés |
| Garde-fous | plafond, budget, détection de boucle |

## Dynamique
1. Appeler le modèle avec l'historique et les outils.
2. S'il demande des outils : les exécuter, ajouter les résultats à l'historique, retourner en 1.
3. Sinon : **réponse finale**.
4. À chaque étape, vérifier les **arrêts**.

## Conséquences
- **Flexible** : s'adapte aux cas imprévus.
- La trace donne une explication de ce qui s'est passé.
{: .plus }

- Coût et durée **variables**.
- Peut boucler, se tromper d'outil, inventer un argument.
- Plus difficile à tester : on teste les **bornes** et on lit des **traces**.
{: .moins }

## Quand l'utiliser
Quand le chemin dépend de l'observation, et qu'une erreur est **réversible** ou validée par un humain.

## Quand l'éviter
Quand un workflow suffit : il est moins cher et plus prévisible.

## Dans la clinique
TD4 : vous écrivez la boucle vous-même, avec ses arrêts, puis vous lisez des traces et cataloguez les échecs.

## Mesurer
Étapes, tokens, durée, motif d'arrêt, taux de réponses appuyées sur une observation.
