---
title: Exposer une ressource REST (validation et erreurs lisibles)
resume: Un record d'entrée validé, un DTO de sortie, une traduction centralisée des erreurs.
td: td1
notions: api-rest-validation, couches-spring, securite-basique
---
## Objectif
Ajouter un endpoint qui valide ce qui entre, renvoie un DTO, et répond avec le bon code d'erreur.

## Squelette (exemple hors clinique)
```java
record NouveauLivre(@NotBlank String titre, @NotBlank String auteur) {}
record LivreDto(Long id, String titre, String auteur) {}

@RestController
@RequestMapping("/api/livres")
class LivreController {
    private final LivreService service;
    LivreController(LivreService service) { this.service = service; }

    @PostMapping
    ResponseEntity<LivreDto> creer(@Valid @RequestBody NouveauLivre corps) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(corps));
    }
}

@RestControllerAdvice
class Erreurs {
    @ExceptionHandler(LivreDejaPresentException.class)
    ResponseEntity<Map<String, String>> conflit(LivreDejaPresentException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
    }
}
```

## Vérifier
```bash
curl -u gribouille:gribouille -X POST localhost:8080/api/livres \
     -H 'Content-Type: application/json' -d '{"titre":"","auteur":"X"}'      # 400
```

## Pièges
- 403 sur un `POST` : CSRF non ignoré pour `/api/**`.
- Renvoyer l'entité JPA : relations paresseuses, boucles de sérialisation. Renvoyez un DTO.
