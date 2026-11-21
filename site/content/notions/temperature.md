---
title: Température et non-déterminisme
resume: Un même prompt peut donner des réponses différentes ; la température règle la dispersion.
cm: CM1 — Fondements : Comment choisir le prochain token ; Les réglages qui comptent
td: td2
---
Le modèle choisit le prochain token au hasard parmi les plus probables. La **température** règle ce hasard : proche de 0, il choisit presque toujours le plus probable ; vers 1, il varie davantage.

- Pour un assistant qui **utilise des outils** : température basse (0,2 à 0,3).
- Pour comparer deux prompts : rejouez chacun plusieurs fois, une seule réponse ne prouve rien.
- Même à 0, deux exécutions peuvent différer : n'écrivez **jamais** de test qui attend une réponse exacte d'un vrai modèle. Utilisez le modèle simulé.
