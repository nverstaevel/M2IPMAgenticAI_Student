---
title: Tokens, fenêtre de contexte et coût
resume: Tout se compte en tokens : ce que le modèle lit, écrit, et ce que vous payez.
cm: CM1 — Fondements : La fenêtre de contexte borne et facture tout ; Outillage : Tout se compte en tokens
td: td2, td3
---
## L'idée
Le modèle lit et écrit des **tokens**. Tout ce qu'il lit — instructions, outils, historique, résultats d'outils — consomme une fenêtre limitée, qui est aussi ce que vous payez.

## Comprendre
{{fig:contexte}}

- 1 token ≈ 3 à 4 caractères en français.
- **Coût** = tokens d'entrée × tarif d'entrée + tokens de sortie × tarif de sortie.
- Les tokens d'entrée sont **renvoyés et facturés à chaque appel** : l'historique et les descriptions d'outils pèsent à chaque tour.
- **Latence** : plus l'entrée est longue, plus la première réponse tarde.
- **Qualité** : un passage utile noyé au milieu d'un long texte est moins bien exploité.

Pour mesurer : `reponse.getMetadata().getUsage()` donne `getPromptTokens()`, `getCompletionTokens()`, `getTotalTokens()`. Si le fournisseur ne les annonce pas, estimez (≈ 1 token pour 4 caractères) et dites-le.

## Dans la clinique
Au TD3, le *grand bestiaire* fait 185 Ko (≈ 46 000 tokens). Tout verser dans le prompt coûte ce prix **à chaque question**, même pour une réponse de trois mots.

## À retenir
- Entrée et sortie comptent toutes deux ; l'entrée est relue à chaque appel.
- Plus la conversation est longue, plus chaque appel est cher.

## Erreurs fréquentes
- Croire que « la réponse est courte donc l'appel est bon marché ».
- Oublier que les descriptions d'outils font partie de l'entrée.

## Auto-test
Q: Pourquoi la 10ᵉ question d'une conversation coûte-t-elle plus que la 1ʳᵉ ?
R: Parce que l'historique (9 échanges) est renvoyé avec la nouvelle question : l'entrée grossit à chaque tour.

Q: Quel est l'effet de 20 outils aux descriptions longues ?
R: Elles s'ajoutent à l'entrée de **chaque** appel : coût et latence augmentent, même si aucun outil n'est utilisé.
