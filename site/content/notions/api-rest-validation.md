---
title: API REST, validation et erreurs
resume: Le contrôleur valide l'entrée et traduit les exceptions métier en codes HTTP.
cm: CM1 — Rappel Spring Boot : Un contrôleur REST ; Valider les entrées, traduire les erreurs
td: td1
---
## L'idée
Le contrôleur **valide** ce qui entre et **traduit** les erreurs métier en codes HTTP lisibles. Il ne décide de rien d'autre.

## Comprendre

{{fig:http_erreurs}}

| Situation | Code | Comment |
|---|---|---|
| corps invalide (nom vide…) | 400 | `@Valid` + contraintes sur le `record` d'entrée |
| entité absente | 404 | exception dédiée |
| conflit de créneau | 409 | `@RestControllerAdvice` qui traduit `ConflitDeCreneauException` |
| non connecté / mauvais rôle | 401 / 403 | Spring Security |

```
POST /api/consultations ─▶ contrôleur ─▶ service ──✗ ConflitDeCreneauException
                          ◀── 409  { "message": "Le soigneur est déjà pris à 10h" }
```

Le **message** d'erreur doit être écrit pour être lu : au TD2, l'outil du modèle le renverra tel quel et le modèle le reformulera à l'utilisateur.

## À retenir
- Entrée : `record` + `@Valid`. Sortie : DTO.
- Un seul endroit (`@RestControllerAdvice`) traduit les exceptions en réponses.

## Erreurs fréquentes
- Renvoyer l'entité JPA directement (relations paresseuses, boucles de sérialisation).
- Renvoyer 500 pour une erreur de l'utilisateur : c'est un 4xx.

## Auto-test
Q: Quel code pour une consultation qui chevauche une autre ?
R: 409 (conflit), avec un message qui dit quel soigneur et quelle heure.
