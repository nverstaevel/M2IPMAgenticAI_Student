---
title: Tester sans vrai modèle
resume: Le profil `simule` rend les tests déterministes, rapides et gratuits.
cm: CM1 — Outillage : Mettre au point sans se perdre ; Symptôme, cause probable, premier geste
td: td2, td3, td4, td5
---
## L'idée
Aucun test n'appelle un vrai modèle. Le profil `simule` le remplace par une réponse **déterministe**.

## Comprendre
Une réponse du modèle simulé de la forme

```
outil:rechercherProprietaires:{"nom":"brume"}
```

lui fait **demander** cet outil avec ces arguments ; tout autre texte est renvoyé comme réponse finale.

On teste donc **ce qui vous appartient** : les outils, la boucle, les arrêts, les garde-fous, la confirmation. On ne teste pas la **qualité** d'un vrai modèle : elle se mesure par une grille de questions et se note dans `JOURNAL.md`.

| À tester sans modèle | À évaluer avec un vrai modèle |
|---|---|
| un outil renvoie « Aucun tarif » pour un acte inconnu | le modèle choisit-il le bon outil ? |
| la boucle s'arrête au plafond | la réponse est-elle pertinente ? |
| une proposition expirée est refusée | le ton est-il celui du Dr Gribouille ? |

## À retenir
- Un test d'agent est **déterministe**, rapide, gratuit.
- Le réseau et les clés n'ont rien à faire dans un test unitaire.

## Auto-test
Q: Comment tester que la boucle s'arrête sur `BOUCLE_DETECTEE` ?
R: Avec le modèle simulé, répéter le même `outil:NOM:{json}` ; vérifier le motif d'arrêt.
