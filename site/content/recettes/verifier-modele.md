---
title: Brancher et vérifier un fournisseur de modèle
resume: Choisir Ollama, LM Studio ou une API, puis prouver en une commande que ça répond.
td: td2
famille: modele
ordre: 1
notions: profils-secrets, tester-sans-modele, chatclient-prompt
---
## Quand l'utiliser
Avant tout TD à partir du TD2, et dès qu'un comportement vous paraît bizarre.

## Les étapes
1. Choisissez **un** fournisseur (qui gère l'appel d'outils) :

| Fournisseur | Mise en route | Profil |
|---|---|---|
| Ollama (défaut) | `ollama pull qwen2.5:7b` | aucun |
| LM Studio | démarrer le serveur local (port 1234) | `lmstudio` |
| Claude / Gemini / Groq | variable `ANTHROPIC_API_KEY` / `GEMINI_API_KEY` / `GROQ_API_KEY` | `claude` / `gemini` / `groq` |
| Aucun modèle | réponses déterministes | `simule` |

2. Pour LM Studio, mettez le **nom exact du modèle** (onglet *Developer*) dans `application-local.yml` (copié depuis `application-local.yml.example`, ignoré par Git).
3. Lancez : `mvn spring-boot:run -Dspring-boot.run.profiles=lmstudio`
4. Contrôlez :

```bash
curl -u gribouille:gribouille localhost:8080/api/diagnostic/modele
# {"modele":"…","statut":"OK","dureeMs":…}
```

## Pièges
- **Clé d'API dans un fichier suivi par Git** : jamais. Variable d'environnement ou `application-local.yml`.
- **Modèle local sans appel d'outils** : le modèle ne rappelle jamais d'outil. Changez de modèle.
- **Port ou URL** : pour LM Studio, l'URL contient `/v1` (`http://localhost:1234/v1`).
- Ne passez pas la séance à chercher : si `statut` n'est pas `OK`, lisez le message d'erreur renvoyé, il dit presque toujours quoi corriger.
