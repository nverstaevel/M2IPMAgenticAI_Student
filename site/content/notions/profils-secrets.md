---
title: Profils Spring, propriétés et secrets
resume: Un profil choisit le fournisseur de modèle ; les clés ne vont jamais dans Git.
cm: CM1 — Rappel Spring Boot : Propriétés, profils, secrets ; Outillage : Trois précautions avec une API hébergée
td: td1, td2, td6
---
- `application.properties` : valeurs par défaut. `application-<profil>.properties` : surcharge activée par `--spring.profiles.active=<profil>` (plusieurs profils séparés par des virgules : `lmstudio,jeu`).
- Les clés d'API viennent de **variables d'environnement** (`${GROQ_API_KEY}`) ou de `application-local.yml`, fichier ignoré par Git.
- Un secret poussé sur GitHub est un secret perdu : révoquez la clé, même si vous supprimez le commit.
- Un profil spécial, `simule`, remplace le modèle par une réponse déterministe : indispensable pour les tests.
