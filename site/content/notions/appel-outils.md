---
title: L'appel d'outils (tool calling)
resume: Le modèle ne fait rien : il demande à l'application d'exécuter une méthode, puis lit le résultat.
cm: CM1 — Fondements : Du modèle à l’agent ; CM2 — Renvoyer peu, clairement
td: td2, td3, td4
famille: outils
ordre: 1
---
## L'idée
Un modèle ne *fait* rien : il **demande** à l'application d'exécuter une méthode, puis lit le résultat pour rédiger sa réponse.

## Comprendre
{{fig:tool_calling}}

En Spring AI : `@Tool(description = …)` sur la méthode, `@ToolParam(description = …)` sur chaque paramètre, `.defaultTools(bean)` sur le client. Le modèle ne voit que : le **nom**, la **description** et les **paramètres**.

**La description est tout.** C'est le seul indice dont le modèle dispose pour choisir.

| Description faible | Description utile |
|---|---|
| « Recherche » | « Cherche des propriétaires par nom. Renvoie au plus 5 lignes (id, nom, téléphone). À utiliser avant de parler d'un propriétaire. » |
| `String nom` | `@ToolParam(description = "Partie du nom, ex. « brume »")` |

**Renvoyez peu** : un outil qui renvoie une table entière sature le contexte.

## Dans la clinique
`rechercherProprietaires`, `consultationsDeLaCreature`, `consulterTarif`… chacun appelle un **service** (voir *Couches Spring*).

## À retenir
- Le modèle demande ; l'application exécute.
- Description = quoi + renvoie quoi + quand + exemple de valeur.
- Résultat court, format stable.

## Erreurs fréquentes
- Outil jamais appelé : description vague, ou modèle sans appel d'outils.
- Argument inventé : pas d'exemple dans `@ToolParam`.
- Outil qui **écrit** directement : à éviter (voir *Confirmation des écritures*).

## Auto-test
Q: Le modèle n'appelle jamais votre outil. Trois pistes ?
R: 1) description trop vague ; 2) le prompt système ne dit pas quand l'utiliser ; 3) le modèle choisi ne gère pas l'appel d'outils.

Q: Qui exécute la méthode Java ?
R: L'application, pas le modèle.
