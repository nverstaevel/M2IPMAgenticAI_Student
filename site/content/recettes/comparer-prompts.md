---
title: Comparer deux prompts ou deux températures
resume: Même grille, plusieurs rejeux, un tableau : on décide avec des chiffres, pas à l'intuition.
td: td2
famille: modele
ordre: 7
notions: temperature, evaluer-agent, chatclient-prompt, tokens-contexte
---
## Objectif
Savoir si la variante B est vraiment meilleure que A.

## La méthode
1. **Fixez** : même modèle, même grille de 6 à 10 questions.
2. **Une seule variable** change (le prompt **ou** la température).
3. **Rejouez** chaque question au moins 3 fois par variante.
4. Notez dans un tableau : bonne réponse (0/1), outil attendu appelé (0/1), tokens, latence.

*(chiffres fictifs, pour la forme)*

| Question | A : réussites / 3 | B : réussites / 3 | Tokens A | Tokens B |
|---|---|---|---|---|
| Q1 | 3 | 3 | 420 | 390 |
| Q2 | 1 | 3 | 510 | 480 |

5. **Conclusion** nuancée : « B réussit plus souvent sur Q2 ; pas de différence ailleurs ; 3 essais restent peu ».

## Pièges
- Comparer sur **une** exécution.
- Changer deux choses à la fois : on ne sait plus laquelle compte.
