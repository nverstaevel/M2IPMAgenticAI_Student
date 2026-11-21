---
title: « Proposer, jamais écrire » : la confirmation humaine
resume: Le modèle prépare une action d'écriture ; seul un humain, par un autre chemin, la valide.
cm: CM1 — Confirmation des écritures ; CM2 — La proposition d’abord ; Écrire dans l’agenda : proposer, puis confirmer
td: td2, td3, td4, td5
---
Un modèle peut mal comprendre, être trompé par un texte piégé, ou halluciner un identifiant. On ne lui donne donc **jamais** le pouvoir d'écrire seul.

1. Un outil `proposer…` **vérifie** les données et enregistre une **proposition** (identifiant, durée de validité) ; il n'écrit rien.
2. L'interface affiche la proposition avec Confirmer / Annuler.
3. Confirmer appelle un **endpoint distinct**, réservé au personnel, que **le modèle ne peut pas appeler**.

Ce principe vaut pour la base (TD2), pour Google Agenda (TD3), pour les agents (TD4, TD5) et pour l'application d'un traitement (validation humaine du dossier PDF).
