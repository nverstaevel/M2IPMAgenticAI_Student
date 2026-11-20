# Agent ReAct de la clinique

Un seul agent raisonne et agit en boucle (ReAct). Il émet lui-même son graphe : une étape du modèle, puis les outils appelés.
Le scénario décide seulement des outils qu'il a le droit d'utiliser.

## Entrées
- demande : "Combien coûte une consultation pour un dragon ?"
- maxEtapes (Étapes maximum) : 8

## Agents
- agent = react : outils dossier, pharmacie, agenda, bestiaire

## Graphe
entrée -> agent -> fin
