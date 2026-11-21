---
title: Traces et graphe d'exécution
resume: Lire ce que l'agent a réellement fait : étapes, outils, tokens, durée.
cm: CM2 — Une ligne par étape, pour comprendre ; CM2 — Lire le graphe d’exécution d’un agent
td: td4, td5, td6
---
La page `/agents` dessine le **graphe réel** d'une exécution. Comment le lire :

1. repérez les **appels d'outils** et leurs arguments : correspondent-ils à la demande ?
2. cherchez les **répétitions** (boucle) et les **retours** (un critique qui rejette) ;
3. regardez les **tokens par étape** : où part le budget ?
4. comparez le graphe **prévu** (pointillés) et le graphe **réel**.

Une trace est un **document d'enquête** : le *catalogue des échecs* du TD4 (boucle, mauvais outil, argument inventé, réponse sans preuve…) sert de grille de lecture.
