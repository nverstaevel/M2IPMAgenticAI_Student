---
title: Port et adaptateur pour outils
aka: Hexagonal architecture, gateway, test double
famille: Outils et sources
niveau: 1
cout: aucun
controle: le code (configuration Spring)
resume: Une interface côté domaine, un adaptateur réel et un adaptateur simulé choisis par configuration.
probleme: Tester et évaluer sans réseau ni compte.
td: td3
notions: port-adaptateur, tester-sans-modele, profils-secrets
recettes: port-adaptateur-profils, appeler-une-api-http
---
## Intention
Isoler les dépendances externes (agenda, API, base tierce) pour que l'agent, les tests et l'évaluation ne dépendent pas d'elles.

## Problème
Un agent qui appelle directement Google Agenda ne peut être testé ni sans compte de service, ni sans réseau, ni de façon reproductible.

## Structure
{{fig:port_adaptateur}}

## Participants
| Participant | Rôle |
|---|---|
| Port (interface) | décrit ce dont le domaine a besoin |
| Adaptateur réel | parle à l'extérieur (`GoogleAgendaAdapter`) |
| Adaptateur simulé | liste en mémoire (`AgendaSimule`) |
| Configuration | `AgendaConfig` choisit l'adaptateur selon `agenda.mode` |
| Outil | ne connaît que le port |

## Dynamique
Au démarrage, Spring crée **un seul** adaptateur, selon la propriété `agenda.mode` (`simule` par défaut). L'outil appelle le port ; le comportement est identique pour lui.

## Conséquences
- Tests et évaluation **rapides, reproductibles, gratuits**.
- Le réel n'est essayé qu'en extension, par choix.
{: .plus }

- Il faut maintenir deux implémentations cohérentes.
- Le simulé peut masquer des cas réels (fuseaux, quotas).
{: .moins }

## Quand l'utiliser
Pour toute source qui nécessite réseau, clé, quota ou compte.

## Quand l'éviter
Pour une dépendance triviale et locale (une simple fonction pure).

## Dans la clinique
`AgendaPort` avec `GoogleAgendaAdapter` / `AgendaSimule` ; `WikipediaClient` avec un `RestClient` de test.

## Mesurer
Durée des tests ; part des tests qui tournent sans réseau (visez 100 %).
