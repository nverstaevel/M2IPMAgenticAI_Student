---
title: Interroger plutôt que charger
resume: Donner au modèle un outil de recherche plutôt que verser tout le corpus dans le contexte.
cm: CM2 — Tout dans le prompt, ou un outil de recherche ? ; Retrouver avant de répondre
td: td3
---
Deux stratégies pour qu'un modèle réponde à partir d'un gros document :

- **A. Tout dans le prompt** : simple, mais coûteux (tokens à chaque question), lent, et la qualité baisse quand le passage utile est noyé (voir *Lost in the Middle* : la position compte).
- **B. Outil de recherche** : le modèle demande des mots-clés, l'outil renvoie 3 petits passages. Moins cher, plus rapide, mais la réponse dépend de la qualité de la recherche.
- **C. Similarité (vecteurs)** : on compare le sens plutôt que les mots.

On ne décide pas « à l'intuition » : on **mesure** (`mesures.csv` : tokens, latence, coût, exactitude) puis on dit à partir de quelle taille A cesse d'être raisonnable.
