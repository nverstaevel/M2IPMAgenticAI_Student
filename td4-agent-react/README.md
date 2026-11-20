# TD4 — L'agent ReAct écrit à la main

Point de départ du TD (sujet distribué en cours). Dossier autonome : importez-le seul dans votre IDE (JDK 21).

## Fourni
le corrigé du TD3, l'outillage de suivi (`Evenement`, `SuiviExecution`, `SuiviController`, `SuiviOutils`, `Jetons`, `ComptageJetons`), la page `/agents`, les contrats `Etape`, `ResultatAgent`, `MotifArret`.

## À écrire
`AgentReactService` (la boucle), `TraceStore`, `AgentController`.

## Lancer
```bash
mvn spring-boot:run          # ajoutez -Dspring-boot.run.profiles=simule pour travailler sans modèle
mvn test
```

## Remarques
Ne modifiez pas l'outillage de suivi. Tests : `AgentReactServiceTest`, `AgentApiTest`.

Notez dans `JOURNAL.md`, au fil du TD : ce que vous avez fait, ce qui a cassé, ce que vous avez compris.
