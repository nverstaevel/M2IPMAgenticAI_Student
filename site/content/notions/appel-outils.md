---
title: L'appel d'outils (tool calling)
resume: Le modèle ne fait rien : il demande à l'application d'exécuter une méthode, puis lit le résultat.
cm: CM1 — Fondements : Du modèle à l’agent ; CM2 — Renvoyer peu, clairement
td: td2, td3
---
Un **outil** est une méthode Java que le modèle peut demander d'appeler. Le déroulé :

1. l'application envoie la question **et** la liste des outils (nom, description, paramètres) ;
2. le modèle répond « appelle `rechercherProprietaires` avec `{"nom":"brume"}` » ;
3. **l'application** exécute la méthode (le modèle n'exécute jamais rien) ;
4. le résultat repart au modèle, qui rédige la réponse (ou demande un autre outil).

En Spring AI : `@Tool(description = …)` sur la méthode, `@ToolParam(description = …)` sur les paramètres, `.defaultTools(bean)` sur le client.

**La description est ce que le modèle lit pour choisir.** Une description vague donne des appels au hasard. Précisez : ce que l'outil fait, ce qu'il renvoie, quand l'utiliser, un exemple de valeur pour chaque paramètre.

**Renvoyez peu** : un outil qui renvoie toute une table sature le contexte. Renvoyez le strict nécessaire, dans un format court.
