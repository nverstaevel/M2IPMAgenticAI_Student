---
title: Tokens, fenêtre de contexte et coût
resume: Tout se compte en tokens : ce que le modèle lit, écrit, et ce que vous payez.
cm: CM1 — Fondements : La fenêtre de contexte borne et facture tout ; Outillage : Tout se compte en tokens
td: td2, td3
---
Un **token** est un morceau de mot (en français, environ 1 token pour 3 à 4 caractères). Le modèle lit des tokens et en produit un par un.

- La **fenêtre de contexte** est la quantité maximale de tokens (entrée + sortie) d'un appel.
- **Coût** = tokens d'entrée × tarif entrée + tokens de sortie × tarif sortie. Les tokens d'entrée comptent **à chaque appel** : l'historique de conversation et les descriptions d'outils sont renvoyés à chaque fois.
- **Latence** : plus l'entrée est longue, plus la première réponse est lente.

Pour mesurer : `reponse.getMetadata().getUsage()` donne `getPromptTokens()`, `getCompletionTokens()`, `getTotalTokens()`. Certains fournisseurs ne les annoncent pas ; le dossier fourni estime alors (≈ 1 token pour 4 caractères).

**Vérifiez que vous avez compris** : pourquoi la stratégie « tout dans le prompt » du TD3 coûte-t-elle plus à chaque question, même si la réponse est courte ?
