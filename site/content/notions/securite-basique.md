---
title: Sécurité : rôles, authentification, CSRF
resume: Deux rôles (PERSONNEL, ADMIN) et une règle simple pour l'API.
cm: CM1 — Rappel Spring Boot : Tester l’API sans navigateur ; Confirmation des écritures
td: td1, td2
famille: spring
ordre: 5
---
## L'idée
Spring Security protège les URL par **rôle**. Deux rôles suffisent ici : `PERSONNEL` et `ADMIN`.

## Comprendre

{{fig:securite_roles}}

- **Authentification** : qui êtes-vous ? (`curl -u gribouille:gribouille …`).
- **Autorisation** : avez-vous le droit ? (rôle).
- **CSRF** : protection contre les formulaires piégés, utile pour les pages web, **gênante pour une API** appelée par `curl` ou JavaScript : un `POST` sans jeton répond 403. On l'ignore donc pour `/api/**`.

| Symptôme | Cause probable |
|---|---|
| 401 | pas d'identifiants |
| 403 sur un `GET` | mauvais rôle |
| 403 sur un `POST` | CSRF non ignoré pour `/api/**` |

## Dans la clinique
Le bouton **Confirmer** du TD2 appelle un endpoint réservé à `PERSONNEL`. **Le modèle n'a aucun outil pour l'appeler** : c'est ce qui rend la confirmation fiable.

## À retenir
- Les droits se vérifient côté serveur, pas dans l'interface.
- Ce que le modèle n'a pas le droit de faire doit être **impossible**, pas seulement « interdit par le prompt ».

## Erreurs fréquentes
- Confondre 401 (qui êtes-vous ?) et 403 (vous n'avez pas le droit).
- Désactiver CSRF partout : seulement pour `/api/**`, jamais pour les pages avec formulaire.
- Mettre un mot de passe en clair ailleurs que dans le TD.

## Auto-test
Q: Un `POST` sur `/api/…` répond 403 alors que vous êtes connecté. Premier réflexe ?
R: Vérifier la configuration CSRF : elle doit être ignorée pour `/api/**`.
