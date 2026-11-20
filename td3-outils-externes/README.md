# TD3 — Outils externes

Point de départ du TD (sujet distribué en cours). Dossier autonome : importez-le seul dans votre IDE (JDK 21).

## Fourni
le corrigé du TD2, `ExperienceContexte` (lit le CSV, écrit les mesures), les fichiers de données (`src/main/resources/donnees`, `experience`), les scripts.

## À écrire
les quatre outils (`OutilsExternes`), `AgendaPort` et ses deux implémentations (`AgendaSimule`, `GoogleAgendaAdapter`), les squelettes `WikipediaClient` et `TarifsExcelReader`, la fonction `mesurer` d'`ExperienceContexte`.

## Lancer
```bash
mvn spring-boot:run          # ajoutez -Dspring-boot.run.profiles=simule pour travailler sans modèle
mvn test
```

## Remarques
Les tests `OutilsExternesTest`, `WikipediaClientTest`, `ExperienceContexteTest` décrivent le comportement attendu.

Notez dans `JOURNAL.md`, au fil du TD : ce que vous avez fait, ce qui a cassé, ce que vous avez compris.
