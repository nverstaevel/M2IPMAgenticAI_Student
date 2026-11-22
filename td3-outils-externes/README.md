# TD3 — Outils externes

Point de départ du TD (sujet distribué en cours). Dossier autonome : importez-le seul dans votre IDE (JDK 21).

## Fourni (prêt à l'emploi)
le corrigé du TD2, les clients des sources externes : `WikipediaClient`, `TarifsExcelReader` (classeur Excel), `CatalogueMedicaments` (requête SQL paramétrée), `AgendaPort` avec `AgendaSimule` (en mémoire) et `GoogleAgendaAdapter` (réel, facultatif) ; `ExperienceContexte` (stratégies A et B, mesures) et `ExperienceController` (lance l'expérience) ; les fichiers de données (`src/main/resources/donnees`, `experience`) et les scripts. L'outil `chercherDansWikipedia` est écrit en entier : c'est votre modèle.

## À écrire
cinq outils de `OutilsExternes` : `consulterTarif`, `stockPharmacie`, `chercherMedicament`, `creneauxLibres`, `proposerEvenementAgenda`. Chacun est annoté de consignes dans le code (blocs « À VOUS »), aux endroits marqués `TODO TD3`. Le reste du TD est de l'analyse : tableau de conception (étape 0), lecture des chiffres, synthèse.

## Lancer
```bash
mvn spring-boot:run                              # ajoutez -Dspring-boot.run.profiles=simule pour travailler sans modèle
mvn test -Dtest=OutilsExternesTest               # les tests disent ce que vos outils doivent répondre
curl -u gribouille:gribouille -X POST "localhost:8080/api/experience/lancer?tailleMax=5000"   # expérience (vrai modèle)
```

## Agenda réel (« Pour aller plus loin », facultatif)
Par défaut `agenda.mode=simule`. La marche à suivre pour Google Agenda (compte de service personnel, agenda de test partagé, `agenda.mode: google`) est à la fin du sujet. La clé JSON ne va jamais dans Git.

## Remarques
Les tests `OutilsExternesTest`, `WikipediaClientTest`, `ExperienceContexteTest` décrivent le comportement attendu.

Notez dans `JOURNAL.md`, au fil du TD : ce que vous avez fait, ce qui a cassé, ce que vous avez compris.
