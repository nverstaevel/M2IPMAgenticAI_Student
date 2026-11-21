# TD1 — La clinique (Spring Boot classique)

Point de départ du TD (sujet distribué en cours). Dossier autonome : importez-le seul dans votre IDE (JDK 21).

## Fourni
pom.xml (versions fixées), configuration, `data.sql`, gabarits Thymeleaf de base (`fragments.html`, `erreur.html`), `JOURNAL.md`.

## À écrire
entités, repositories, `CliniqueService`, contrôleurs, DTO, gabarits de pages, configuration de sécurité, tests.

## Lancer
```bash
mvn spring-boot:run
mvn test
```

## Remarques
Il n'y a pas de tests fournis : les écrire fait partie du TD. L'application ne démarre pas tant que vos entités ne correspondent pas aux tables de `data.sql` (erreur attendue au début : *Table ESPECE not found*). Le sujet, étape 1, explique comment lire `data.sql`.

Notez dans `JOURNAL.md`, au fil du TD : ce que vous avez fait, ce qui a cassé, ce que vous avez compris.
