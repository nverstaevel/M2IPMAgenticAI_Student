---
title: Sécurité : rôles, authentification, CSRF
resume: Deux rôles (PERSONNEL, ADMIN) et une règle simple pour l'API.
cm: CM1 — Rappel Spring Boot : Tester l’API sans navigateur ; Confirmation des écritures
td: td1, td2
---
La clinique a des utilisateurs de test (`gribouille` / `gribouille`). Spring Security protège les URL par rôle.

- Pour une API appelée par `curl` ou par JavaScript, on **ignore CSRF sur `/api/**`** ; sinon les `POST` répondent 403.
- Le bouton *Confirmer* du TD2 appelle un endpoint réservé au rôle `PERSONNEL`. **Le modèle n'a aucun outil pour l'appeler** : c'est ce qui rend la confirmation fiable.
- 401/403 sur une API : vous n'êtes pas connecté, ou pas avec le bon rôle (`-u gribouille:gribouille`).
