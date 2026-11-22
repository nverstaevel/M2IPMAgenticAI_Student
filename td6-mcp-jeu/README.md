# TD6 — Le jeu de la clinique en MCP

Point de départ du TD (sujet distribué en cours). Dossier autonome : importez-le seul dans votre IDE (JDK 21).

## Fourni (prêt à l'emploi)
le corrigé du TD5, le jeu (`../jeu-bestiaire`, **à ne pas modifier**), **`JeuMcp`** et le groupe d'outils `jeu` (`OutilsDisponibles`), **`AgentsJeu`** (les agents de l'équipe : diagnosticien, gardien, apothicaire, avec des prompts de départ minimaux), le lanceur de partie (`JoueurService`, `JoueurController`), les dépendances du starter client MCP, `application-jeu.properties`, `scenarios/_modele.md`.

## À écrire
- les trois prompts de rôle dans `AgentsJeu` (`PROMPT_DIAGNOSTICIEN`, `PROMPT_GARDIEN`, `PROMPT_APOTHICAIRE`, marqués `TODO TD6`) ;
- vos scénarios `.md` (Gribouille seul, l'équipe, **votre architecture**) ;
- `ANALYSE.md` : affirmation, mesure, limite.

## Lancer
```bash
mvn spring-boot:run          # ajoutez -Dspring-boot.run.profiles=simule pour travailler sans modèle
mvn test
```

## Remarques
Lancez d'abord le jeu (`mvn -f ../jeu-bestiaire/pom.xml spring-boot:run`, port 8090), puis la clinique avec le profil `jeu` : `mvn spring-boot:run -Dspring-boot.run.profiles=<modèle>,jeu`. `LiaisonMcpTest` joue contre un vrai jeu : il n'est activé que par `-Djeu.reel=true`.

Notez dans `JOURNAL.md`, au fil du TD : ce que vous avez fait, ce qui a cassé, ce que vous avez compris.
