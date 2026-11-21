---
title: Tester sans vrai modèle
resume: Le profil `simule` rend les tests déterministes, rapides et gratuits.
cm: CM1 — Outillage : Mettre au point sans se perdre ; Symptôme, cause probable, premier geste
td: td2, td3, td4, td5
---
Aucun test n'appelle un vrai modèle. Le profil `simule` remplace le modèle par `ModeleSimule` : une réponse déterministe.

- `outil:rechercherProprietaires:{"nom":"brume"}` : le modèle simulé **demande** cet outil avec ces arguments.
- On teste donc ce qui vous appartient : les outils, la boucle, les arrêts, les garde-fous, la confirmation.
- Ce qu'on ne teste pas : la **qualité** des réponses d'un vrai modèle. Elle se mesure par une grille de questions et se note dans `JOURNAL.md`.
