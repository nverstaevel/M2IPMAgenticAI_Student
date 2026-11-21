---
title: La boucle d'agent ReAct
resume: Raisonner, agir (appeler un outil), observer le résultat, recommencer jusqu'à pouvoir répondre.
cm: CM2 — Penser, agir, observer ; Une exécution ReAct, dans le code
td: td4
---
## L'idée
Un **agent** est une boucle autour du modèle : il raisonne, agit (appelle un outil), observe le résultat, et recommence jusqu'à pouvoir répondre.

## Comprendre
{{fig:react}}

Une **étape** = un appel au modèle + l'exécution des outils qu'il a demandés. Le modèle ne lance jamais les outils : c'est `ToolCallingManager` qui le fait, et l'historique renvoyé alimente le tour suivant.

```
« Quel est le tarif d'un vaccin pour un yéti, et le stock de baume ? »
  étape 1 : pense « il me faut le tarif »   → outil consulterTarif(Vaccin, Yéti)  → 30,00 €
  étape 2 : pense « puis le stock »         → outil stockPharmacie(Baume)         → 12 pots
  étape 3 : plus d'outil à demander         → réponse finale
```

La « pensée » n'est pas exposée séparément par les API modernes : vous la **sollicitez** dans le prompt et vous la lisez **avec esprit critique** (un raisonnement écrit n'est pas une explication fidèle).

## À retenir
- Agent = modèle + outils + **boucle**.
- Sans bornes, la boucle peut tourner indéfiniment (voir *Les arrêts d'un agent*).

## Erreurs fréquentes
- Confondre « le modèle a appelé l'outil » et « le modèle a exécuté l'outil ».
- Croire que le raisonnement affiché explique la décision.

## Auto-test
Q: Combien d'étapes dans l'exemple ci-dessus ?
R: Trois : deux avec outil, une réponse finale.
