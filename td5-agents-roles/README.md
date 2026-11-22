# TD5 — Agents à rôles et dossier PDF

Point de départ du TD (sujet distribué en cours). Dossier autonome : importez-le seul dans votre IDE (JDK 21).

## Fourni (prêt à l'emploi)
le corrigé du TD4 (votre agent ReAct), le moteur de scénarios (`scenario` : `Orchestrateur`, `ScenarioLoader`, `Agent`, `Entree`, `Resultat`…), **`VeterinaireAgent` et `GardienDuBestiaire`** (avec leurs prompts), les beans `veterinaire`, `gardien`, `travailleur` et `synthese` (`scenario/AgentsExemples`), `DuoService`, `SuperviseurAgent`, **`DossierPdfService` complet**, la page `/agents` en mode « Scénario », `SCENARIOS.md`, `scenarios/_modele.md`, `scenarios/react-clinique.md`, `cas-bestiaire.csv`, les contrats `Diagnostic`, `Critique`, `Verdict`, `TraiterConsultationWorkflow` (workflow classique, à lancer et à comparer).

## À écrire
- le bean `planificateur` (`TODO TD5` dans `scenario/AgentsExemples`) : son prompt est votre conception ;
- deux fichiers `.md` dans `src/main/resources/scenarios` : le duo et le superviseur (le sujet donne leur contenu à lire et à comprendre) ;
- une amélioration **mesurée** d'un prompt (étape 4).

Le reste du TD est de l'architecture et de l'analyse : prédictions, graphes, mesures, comparaison au workflow classique.

## Lancer
```bash
mvn spring-boot:run          # ajoutez -Dspring-boot.run.profiles=simule pour travailler sans modèle
mvn test
```

## Remarques
Lisez `SCENARIOS.md` avant d'écrire votre premier scénario. Tests : `DuoServiceTest`, `RolesApiTest`, `SuiviRolesTest`, `scenario/ScenarioTest`. Les évaluations des étapes 1 à 6 se font avec votre LLM.

Notez dans `JOURNAL.md`, au fil du TD : ce que vous avez fait, ce qui a cassé, ce que vous avez compris.
