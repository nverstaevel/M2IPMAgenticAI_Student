---
title: Outil de recherche (interroger plutôt que charger)
aka: Just-in-time context, retrieval tool, RAG par outil
famille: Outils et sources
niveau: 2
cout: faible à moyen
controle: le modèle choisit les mots-clés, le code cherche
resume: Au lieu de verser un corpus entier dans le prompt, le modèle interroge une recherche qui renvoie quelques passages.
probleme: Répondre à partir d'un corpus trop gros pour le prompt, ou trop cher à y mettre.
td: td3
notions: interroger-plutot-que-charger, tokens-contexte, evaluer-agent
recettes: mesurer-tokens, lire-un-excel, comparer-prompts
---
## Intention
Garder le contexte **court** en n'apportant au modèle que les passages utiles, au moment où il en a besoin.

## Problème
Le grand bestiaire fait 185 Ko. Le verser dans le prompt coûte cher **à chaque question**, ralentit, et noie le passage utile.

## Structure
{{fig:strategies_contexte}}

## Participants
| Participant | Rôle |
|---|---|
| Corpus | document source |
| Outil de recherche | renvoie les 3 meilleurs passages pour des mots-clés |
| Modèle | formule la requête, rédige la réponse |

## Dynamique
1. Le modèle appelle `rechercher("dragon aloès")`.
2. L'outil renvoie trois extraits courts.
3. Le modèle répond à partir de ces extraits, ou **relance** une recherche.

## Conséquences
- Moins de tokens, moins de latence, meilleur passage utile.
- Passe à l'échelle.
{: .plus }

- La réponse dépend de la qualité de la recherche (mots-clés, découpage).
- Un modèle peut ne pas chercher quand il le faut.
{: .moins }

## Quand l'utiliser
Corpus **grand** par rapport à la fenêtre ou au budget.

## Quand l'éviter
Petit corpus (quelques Ko) : tout dans le prompt est plus simple et souvent plus fiable.

## Variantes
Recherche **par similarité** (vecteurs) : trouve le sens plutôt que les mots exacts.

## Dans la clinique
TD3, étape 5 : deux stratégies mesurées dans `mesures.csv` (tokens, latence, coût, exactitude) ; vous cherchez le seuil à partir duquel la stratégie A devient déraisonnable.

## Mesurer
Tokens d'entrée, latence, coût, exactitude, pour **les deux** stratégies, sur les mêmes questions.
