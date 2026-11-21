---
title: API REST, validation et erreurs
resume: Le contrôleur valide l'entrée et traduit les exceptions métier en codes HTTP.
cm: CM1 — Rappel Spring Boot : Un contrôleur REST ; Valider les entrées, traduire les erreurs
td: td1
---
- `@RestController`, `@GetMapping`, `@PostMapping`, `@RequestBody`, `@PathVariable`.
- **Validation** : `@Valid` sur le corps, contraintes (`@NotBlank`, `@Size`…) sur le `record` d'entrée.
- **Erreurs** : un `@RestControllerAdvice` traduit `ConflitDeCreneauException` en 409, une entité absente en 404. Le message d'erreur doit être lisible : au TD2, il remontera jusqu'à la conversation du chat.
- **Test sans navigateur** : `MockMvc` (voir l'étape 7 du TD1) ou `curl -u utilisateur:motdepasse …`.
