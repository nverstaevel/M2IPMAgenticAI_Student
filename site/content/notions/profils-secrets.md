---
title: Profils Spring, propriétés et secrets
resume: Un profil choisit le fournisseur de modèle ; les clés ne vont jamais dans Git.
cm: CM1 — Rappel Spring Boot : Propriétés, profils, secrets ; Outillage : Trois précautions avec une API hébergée
td: td2, td6
---
## L'idée
Un **profil** Spring change la configuration sans changer le code. C'est ainsi que l'on choisit le fournisseur de modèle, et que l'on remplace le vrai modèle par un simulé.

## Comprendre

{{fig:profils_config}}

```
application.properties            valeurs par défaut
application-lmstudio.properties   surcharge activée par  --spring.profiles.active=lmstudio
application-local.yml             vos réglages et secrets (ignoré par Git) : profil « local »
```
- Plusieurs profils se cumulent : `lmstudio,jeu`. En cas de conflit, le **dernier** profil de la liste l'emporte.
- `application-local.yml` n'est lu **que si le profil `local` est actif** : `--spring.profiles.active=lmstudio,local`. Sans lui, vos réglages sont ignorés, sans message d'erreur.
- Les clés d'API viennent de **variables d'environnement** (`${GROQ_API_KEY}`) ou de `application-local.yml`.
- **Un secret poussé sur GitHub est un secret perdu** : révoquez la clé, même si vous supprimez le commit.

## Dans la clinique
| Profil | Effet |
|---|---|
| `lmstudio`, `groq`, `claude`, `gemini` | choisit le fournisseur |
| `simule` | modèle simulé déterministe (tests) |
| `persistant` | mémoire de conversation en base |
| `jeu` | active le client MCP (TD6) |

## Erreurs fréquentes
- Deux profils de **modèle** à la fois.
- Oublier d'activer le profil `local` : votre `application-local.yml` existe mais n'est pas lu.
- Faute de frappe dans le nom du profil : aucune erreur, simplement rien ne change. Cherchez *The following profiles are active* dans le log.

## Auto-test
Q: Où mettre une clé d'API ?
R: Dans une variable d'environnement ou dans `application-local.yml` (ignoré par Git), jamais dans un fichier suivi.
