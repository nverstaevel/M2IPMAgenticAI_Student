# TD3 — Outils externes

Point de départ du TD (sujet distribué en cours). Dossier autonome : importez-le seul dans votre IDE (JDK 21).

## Fourni
le corrigé du TD2, `TarifsExcelReader` (lecture du classeur Excel) et `GoogleAgendaAdapter` (client Google Agenda réel), `ExperienceContexte` (lit le CSV, écrit les mesures), les fichiers de données (`src/main/resources/donnees`, `experience`), les scripts.

## À écrire
les outils de `OutilsExternes` (Wikipédia, tarifs et stock Excel, médicaments, agenda), `WikipediaClient`, `AgendaSimule` (l'interface `AgendaPort` est fournie), la fonction `mesurer` d'`ExperienceContexte`. Les endroits sont marqués `TODO TD3`.

## Lancer
```bash
mvn spring-boot:run          # ajoutez -Dspring-boot.run.profiles=simule pour travailler sans modèle
mvn test
```

## Agenda réel (extension)
Par défaut `agenda.mode=simule` : l'agenda est `AgendaSimule`, en mémoire. Pour essayer Google Agenda, utilisez votre propre compte Google : créez un compte de service dans votre propre projet Google Cloud (gratuit) : activez l'API Google Calendar, créez le compte de service, puis générez une clé JSON (« Clés » → « Ajouter une clé »). Créez ensuite un agenda de test vide dans votre Google Agenda et partagez-le avec l'adresse du compte de service (`…@….iam.gserviceaccount.com`, droit « Apporter des modifications aux événements »), puis renseignez dans `application-local.yml` : `agenda.mode: google`, `agenda.google.calendar-id`, `agenda.google.credentials-file`. Le fichier de clés JSON ne va jamais dans Git.

## Remarques
Les tests `OutilsExternesTest`, `WikipediaClientTest`, `ExperienceContexteTest` décrivent le comportement attendu.

Notez dans `JOURNAL.md`, au fil du TD : ce que vous avez fait, ce qui a cassé, ce que vous avez compris.
