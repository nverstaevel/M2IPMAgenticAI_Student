---
title: Interroger plutôt que charger
resume: Donner au modèle un outil de recherche plutôt que verser tout le corpus dans le contexte.
cm: CM2 — Tout dans le prompt, ou un outil de recherche ? ; Retrouver avant de répondre
td: td3
---
## L'idée
Pour qu'un modèle réponde à partir d'un gros document, deux voies : tout verser dans le prompt, ou lui donner un **outil de recherche** qui renvoie quelques passages.

## Comprendre
{{fig:strategies_contexte}}

| Stratégie | + | − |
|---|---|---|
| **A** tout dans le prompt | simple | cher à chaque question, lent, passage utile noyé |
| **B** outil de recherche | peu de tokens, rapide | la qualité dépend de la recherche |
| **C** similarité (vecteurs) | trouve le sens, pas seulement les mots | infrastructure supplémentaire |

On ne tranche pas à l'intuition : on **mesure** (`mesures.csv` : tokens, latence, coût, exactitude), puis on dit **à partir de quelle taille** A cesse d'être raisonnable.

## À retenir
- La taille du corpus décide : petit → A suffit ; grand → B.
- Mesurer, puis conclure avec des chiffres… et leurs limites.

## Auto-test
Q: Le corpus passe de 10 Ko à 1 Mo. Qu'est-ce qui change le plus pour A ?
R: Le coût et la latence par question, proportionnels à la taille, et le risque que le passage utile soit noyé.
