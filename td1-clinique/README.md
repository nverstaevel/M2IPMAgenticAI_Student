# TD1 — La clinique (Spring Boot classique)

Point de départ du TD (sujet distribué en cours). Dossier autonome : importez-le seul dans votre IDE (JDK 21).

## Fourni (prêt à l'emploi)
`pom.xml` (versions fixées), configuration, `data.sql`, DTO, exceptions, **dépôts Spring Data**, contrôleurs REST et web, gabarits Thymeleaf, **configuration de sécurité**, **tests** (`CliniqueServiceTest`, `ApiTest` : ils décrivent le comportement attendu), les entités `Espece` et `Creature` (modèle de relation), `JOURNAL.md`.

## À écrire (6 `TODO TD1`)
- `Proprietaire`, `Soigneur`, `Consultation` : les relations JPA (le côté « plusieurs » porte la clé) ;
- `CliniqueService` : `planifierConsultation` (règle de chevauchement) et `enregistrerDiagnostic`.

Le reste du TD est de la conception et de la lecture : tableau de relations, prédictions de l'API et de la sécurité, un test de plus.

## Lancer
```bash
mvn spring-boot:run
mvn test -Dtest=CliniqueServiceTest      # puis ApiTest, puis mvn verify
```

## Remarques
L'application ne démarre pas tant que les relations ne sont pas mappées (erreur attendue au début), et les tests échouent tant que les `TODO TD1` ne sont pas écrits. Le sujet, étape 1, explique comment lire `data.sql`.

Notez dans `JOURNAL.md`, au fil du TD : ce que vous avez fait, ce qui a cassé, ce que vous avez compris.
