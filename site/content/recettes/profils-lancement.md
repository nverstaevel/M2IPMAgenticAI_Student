---
title: Lancer un TD avec plusieurs profils
resume: Combiner un profil de modèle, `jeu` ou `persistant`, depuis Maven ou l'IDE.
td: td2, td6
notions: profils-secrets
---
## Quand l'utiliser
Dès qu'une fonctionnalité dépend d'un profil (`persistant` au TD2, `jeu` au TD6).

## Maven
```bash
cd td6-mcp-jeu
mvn spring-boot:run -Dspring-boot.run.profiles=lmstudio,jeu
# ou
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=lmstudio,jeu
```

## IDE (Spring Tools)
*Run Configurations… → Spring Boot App → onglet Profile*, ou argument de programme `--spring.profiles.active=lmstudio,jeu`.

## Variables d'environnement
```bash
export GROQ_API_KEY=…        # Linux / macOS
$env:GROQ_API_KEY="…"        # PowerShell
```

## Pièges
- Un **seul** profil de modèle à la fois (`lmstudio` **ou** `groq`…).
- Les profils se **cumulent** : `lmstudio,jeu`.
- Un profil qui n'existe pas ne provoque pas d'erreur : vérifiez l'orthographe dans les logs de démarrage (*The following profiles are active*).
