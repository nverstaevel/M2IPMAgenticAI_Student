---
title: La boucle d'agent ReAct
resume: Raisonner, agir (appeler un outil), observer le résultat, recommencer jusqu'à pouvoir répondre.
cm: CM2 — Penser, agir, observer ; Une exécution ReAct, dans le code
td: td4
---
Un **agent** est une boucle autour du modèle :

```
Penser (une phrase) → Agir (appel d'outil) → Observer (résultat) → Penser … → Réponse finale
```

Une **étape** = un appel au modèle suivi de l'exécution des outils qu'il a demandés. Le modèle ne lance jamais les outils : c'est `ToolCallingManager` qui le fait à votre demande, et l'historique renvoyé alimente le tour suivant.

La « pensée » n'est pas exposée séparément par les API modernes : vous la **sollicitez** dans le prompt et vous la lisez **avec esprit critique** (un raisonnement écrit n'est pas une explication fidèle).
