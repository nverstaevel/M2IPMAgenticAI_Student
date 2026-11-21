---
title: Température et non-déterminisme
resume: Un même prompt peut donner des réponses différentes ; la température règle la dispersion.
cm: CM1 — Fondements : Comment choisir le prochain token ; Les réglages qui comptent
td: td2
---
## L'idée
À chaque mot, le modèle tire au sort parmi les suites probables. La **température** règle ce hasard : basse = presque toujours le choix le plus probable ; haute = plus de variété.

## Comprendre
{{fig:temperature}}

| Usage | Température |
|---|---|
| assistant qui utilise des outils, extraction de données | basse (0,2 à 0,3) |
| rédaction créative, reformulation | plus haute (0,7 et +) |

**Même à 0**, deux exécutions peuvent différer (arrondis, parallélisme du serveur). Donc :
- une seule réponse ne prouve rien : rejouez plusieurs fois pour comparer deux prompts ;
- **aucun test automatique** ne doit attendre une phrase exacte d'un vrai modèle (utilisez le modèle simulé).

## À retenir
- Basse pour agir, haute pour imaginer.
- Variabilité ≠ bug : c'est la nature du modèle.

## Auto-test
Q: Votre test passe 9 fois sur 10 avec un vrai modèle. Que faire ?
R: Ne pas tester la phrase : tester ce qui vous appartient (outil appelé, argument, motif d'arrêt) avec le modèle simulé.
