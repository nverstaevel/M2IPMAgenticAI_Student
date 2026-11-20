# TD5 — Agents à rôles et dossier PDF

Point de départ du TD (sujet distribué en cours). Dossier autonome : importez-le seul dans votre IDE (JDK 21).

## Fourni
le corrigé du TD4 (votre agent ReAct), le moteur de scénarios (`scenario` : `Orchestrateur`, `ScenarioLoader`, `Agent`, `Entree`, `Resultat`…), `DuoService`, `SuperviseurAgent`, la page `/agents` en mode « Scénario », `SCENARIOS.md`, `scenarios/_modele.md`, `scenarios/react-clinique.md`, `cas-bestiaire.csv`, les contrats `Diagnostic`, `Critique`, `Verdict`, `TraiterConsultationWorkflow` (workflow classique, à lancer et à comparer), et dans `DossierPdfService` tout sauf le contenu du dossier (mise en page, en-tête, annexe technique).

## À écrire
`VeterinaireAgent`, `GardienDuBestiaire`, le bloc par tour et la décision de `DossierPdfService` (marqués `TODO TD5`), les beans `Agent` de vos rôles (dans `scenario/AgentsExemples`), vos fichiers `.md` dans `src/main/resources/scenarios`.

## Lancer
```bash
mvn spring-boot:run          # ajoutez -Dspring-boot.run.profiles=simule pour travailler sans modèle
mvn test
```

## Remarques
Lisez `SCENARIOS.md` avant d'écrire votre premier scénario. Tests : `DuoServiceTest`, `RolesApiTest`, `SuiviRolesTest`, `scenario/ScenarioTest`.

Notez dans `JOURNAL.md`, au fil du TD : ce que vous avez fait, ce qui a cassé, ce que vous avez compris.
