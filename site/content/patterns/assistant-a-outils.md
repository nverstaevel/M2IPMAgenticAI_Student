---
title: Assistant à outils
aka: Tool-using assistant, Function calling
famille: Outils et sources
niveau: 1
cout: faible à moyen
controle: le modèle choisit l'outil, le code l'exécute
resume: Le modèle répond en appelant des méthodes de l'application plutôt qu'en devinant.
probleme: Répondre sur des données que le modèle ne connaît pas.
td: td2, td3
notions: appel-outils, couches-spring, chatclient-prompt
recettes: ajouter-un-outil, ecrire-prompt-systeme, tester-avec-modele-simule
---
## Intention
Donner au modèle des **mains** pour lire les données de l'application, au lieu de le laisser inventer.

## Problème
Un modèle ne connaît ni vos créatures, ni vos tarifs, ni l'état de votre base. Sans outil, il répond avec assurance… et à tort.

## Structure
{{fig:tool_calling}}

## Participants
| Participant | Rôle |
|---|---|
| Modèle | choisit un outil et ses arguments |
| Outil (`@Tool`) | méthode Java décrite pour le modèle |
| Service métier | porte les règles ; l'outil ne fait que l'appeler |
| Application | exécute l'outil et renvoie le résultat |

## Dynamique
1. La question part avec la **liste des outils** (nom, description, paramètres).
2. Le modèle **demande** un appel.
3. L'application exécute la méthode.
4. Le résultat repart au modèle, qui rédige la réponse (ou demande un autre outil).

## Conséquences
- Réponses **ancrées** dans les données réelles.
- Chaque description d'outil est relue à chaque appel : un coût.
{: .plus }

- Un outil mal décrit n'est pas appelé, ou l'est mal.
- Un outil qui écrit ouvre la porte aux erreurs : voir *Confirmation humaine*.
{: .moins }

## Quand l'utiliser
Dès que la réponse dépend de **données de l'application**.

## Quand l'éviter
Si la question ne demande que des connaissances générales, ou si un seul appel déterministe suffit (alors un **workflow**).

## Dans la clinique
TD2 : cinq outils de lecture sur les services du TD1 (`rechercherProprietaires`, …). TD3 : quatre outils externes (Wikipédia, tarifs, médicaments, agenda).

## Mesurer
Taux de bon outil choisi sur une grille de questions ; tokens ajoutés par les descriptions.
