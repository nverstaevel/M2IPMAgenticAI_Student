---
title: Superviseur–travailleurs
aka: Orchestrator–workers, planner–executor, map–reduce d'agents
famille: Plusieurs agents
niveau: 3
cout: élevé
controle: un agent planifie, d'autres exécutent
resume: Un planificateur découpe la tâche, des travailleurs l'exécutent avec leurs outils, une synthèse assemble.
probleme: Une tâche qui se découpe en sous-tâches non connues à l'avance.
td: td5, td6
notions: agents-roles, moindre-privilege, workflow-ou-agent, scenarios-md
recettes: ecrire-un-scenario, donner-ses-outils-a-un-role, comparer-deux-architectures
---
## Intention
Répartir une tâche entre des agents **spécialisés**, coordonnés par un planificateur.

## Problème
Une demande complexe ne se traite pas d'un bloc : il faut la découper, et le découpage dépend de la demande.

## Structure
{{fig:superviseur}}

## Participants
| Participant | Rôle |
|---|---|
| Planificateur | produit un plan (liste de tâches) |
| Travailleurs | exécutent une tâche avec **leurs** outils |
| Synthèse | assemble les résultats |

## Dynamique
1. Le planificateur lit la demande et produit un plan.
2. Chaque tâche est confiée à un travailleur (`=>` dans le scénario).
3. La synthèse assemble les résultats.

## Conséquences
- Chaque travailleur a un prompt **court** et peu d'outils.
- Parallélisable.
{: .plus }

- Beaucoup d'appels : coût et latence.
- Un mauvais plan ruine tout.
- Difficile à déboguer sans traces.
{: .moins }

## Quand l'utiliser
Sous-tâches **indépendantes** et imprévisibles.

## Quand l'éviter
Si les sous-tâches sont toujours les mêmes : un **workflow** fait mieux.

## Dans la clinique
TD5, étape 6 : un seul bean `travailleur`, trois nœuds aux outils différents. TD6 : planificateur, diagnosticien, apothicaire.

## Mesurer
Qualité du plan, tokens par rôle, durée totale, comparée à un seul agent.
