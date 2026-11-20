# TD2 — Un chat qui interroge la clinique

Point de départ du TD (sujet distribué en cours). Dossier autonome : importez-le seul dans votre IDE (JDK 21).

## Fourni
le corrigé du TD1, les dépendances Spring AI, les profils de fournisseurs (`application-*.properties`), `ModeleSimule`, la page `chat.html`, le diagnostic du modèle.

## À écrire
la configuration du `ChatClient` (`ChatConfig`), le prompt système (`PromptsGribouille`), les outils (`OutilsClinique`), le triage, la proposition et sa confirmation (`PropositionService`, `ChatController`).

## Lancer
```bash
mvn spring-boot:run          # ajoutez -Dspring-boot.run.profiles=simule pour travailler sans modèle
mvn test
```

## Remarques
Tant que `ChatConfig` n'est pas écrit, l'application ne démarre pas (message « À écrire »). Les tests fournis (`ChatApiTest`, `PropositionServiceTest`, `OutilsCliniqueTest`…) passent quand le TD est terminé ; sans modèle, utilisez le profil `simule`.

Notez dans `JOURNAL.md`, au fil du TD : ce que vous avez fait, ce qui a cassé, ce que vous avez compris.
