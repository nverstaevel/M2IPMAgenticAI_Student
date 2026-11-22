# TD2 — Un chat qui interroge la clinique

Point de départ du TD (sujet distribué en cours). Dossier autonome : importez-le seul dans votre IDE (JDK 21).

## Fourni (prêt à l'emploi)
le corrigé du TD1, les dépendances Spring AI, les profils de fournisseurs (`application-*.properties`), `ModeleSimule`, la page `chat.html`, le diagnostic du modèle, **`ChatConfig`**, **`ChatController`**, **`PropositionService`**, le prompt `BASE`, deux outils de lecture écrits en entier (`rechercherProprietaires`, `trouverProprietaire`) et l'outil d'écriture `proposerConsultation`, les tests.

## À écrire (4 `TODO TD2`)
- `PromptsGribouille.STRICTE` : le prompt strict ;
- `OutilsClinique` : `creaturesDuProprietaire`, `consultationsDeLaCreature`, `soigneursPourEspece` (une ligne chacun).

Le reste du TD est de la conception et de l'évaluation avec **votre LLM** : tableau d'analyse des outils, comparaison de prompts, grille de questions, cas limites de la confirmation.

## Lancer
```bash
mvn spring-boot:run          # ajoutez -Dspring-boot.run.profiles=simule pour travailler sans modèle
mvn test
```

## Remarques
Les tests (`OutilsCliniqueTest`, `PropositionServiceTest`, `ChatApiTest`…) décrivent le comportement attendu et passent quand les `TODO TD2` sont écrits ; sans modèle, utilisez le profil `simule`. Les évaluations de l'étape 4 se font avec un vrai modèle.

Notez dans `JOURNAL.md`, au fil du TD : ce que vous avez fait, ce qui a cassé, ce que vous avez compris.
