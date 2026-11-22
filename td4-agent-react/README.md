# TD4 — L'agent ReAct écrit à la main

Point de départ du TD (sujet distribué en cours). Dossier autonome : importez-le seul dans votre IDE (JDK 21).

## Fourni
le corrigé du TD3, l'outillage de suivi (`Evenement`, `SuiviExecution`, `SuiviController`, `SuiviOutils`, `Jetons`, `ComptageJetons`), la page `/agents`, les contrats `Etape`, `ResultatAgent`, `MotifArret`, le stockage des traces (`TraceStore`), les endpoints (`AgentController`), et dans `AgentReactService` tout sauf le cœur de la boucle : le suivi du graphe, l'arrêt sur budget de tokens et l'arrêt sur erreur sont déjà écrits.

## À écrire
uniquement le cœur de `AgentReactService.executer`, aux endroits marqués `TODO TD4` (le sujet donne le code de chaque endroit : à poser, puis à expliquer) : l'appel du modèle, la réponse finale, l'exécution des outils et l'historique (étape 1), puis les arrêts sur plafond d'étapes et sur boucle détectée (étape 2).

## Lancer
```bash
mvn spring-boot:run          # ajoutez -Dspring-boot.run.profiles=simule pour travailler sans modèle
mvn test
```

## Remarques
Ne modifiez pas l'outillage de suivi. Tests : `AgentReactServiceTest`, `AgentApiTest`.

Notez dans `JOURNAL.md`, au fil du TD : ce que vous avez fait, ce qui a cassé, ce que vous avez compris.
