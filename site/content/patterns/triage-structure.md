---
title: Triage structuré
aka: Routage, classification, structured output, router
famille: Outils et sources
niveau: 1
cout: faible
controle: le modèle classe, le code route
resume: Un appel isolé transforme un texte libre en objet typé, que le code utilise pour décider de la suite.
probleme: Passer d'une phrase libre à une décision exploitable par le code.
td: td2, td5
notions: sortie-structuree, chatclient-prompt, tokens-contexte
recettes: sortie-structuree, ecrire-prompt-systeme
---
## Intention
Utiliser le modèle comme **extracteur / classifieur** : texte libre en entrée, objet Java typé en sortie.

## Problème
Une demande « Ma licorne boite depuis ce matin » doit devenir `(espèce, symptôme, urgence)` pour que le code oriente la suite — sans parser une phrase à la main.

## Structure
{{fig:sortie_structuree}}

## Participants
| Participant | Rôle |
|---|---|
| `ChatClient` isolé | sans mémoire, sans outils |
| `record` + `enum` | le format attendu |
| Code appelant | route selon `urgence` |

## Dynamique
1. `.entity(Triage.class)` ajoute le schéma au prompt.
2. Le modèle remplit les champs.
3. Le code lit `urgence` et décide : file prioritaire, outil, agent.

## Conséquences
- Sortie **vérifiable** par le compilateur et testable.
- Appel court et peu coûteux.
{: .plus }

- Le modèle peut quand même se tromper de format ou de classe.
- Il faut prévoir un échec propre (nouvel essai, valeur par défaut).
{: .moins }

## Quand l'utiliser
Pour **aiguiller** : classer, extraire des champs, choisir une branche.

## Quand l'éviter
Si la décision est déjà évidente par des règles simples (un `if` suffit).

## Dans la clinique
TD2 : triage en `Triage`. TD5 : les agents produisent des `Diagnostic` et `Critique` structurés pour pouvoir être **chaînés**.

## Mesurer
Taux de format valide ; exactitude de la classe sur un jeu d'exemples.
