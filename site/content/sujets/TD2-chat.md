# TD2 — Un chat qui interroge l'application

## Objectifs de la séance

Donner une voix à la clinique. Vous branchez un modèle sur l'application avec Spring AI, vous lui donnez une personnalité, une mémoire et des outils qui interrogent vos services, puis vous traitez la seule action d'écriture avec prudence : le modèle propose, un humain confirme.

À la fin de la séance, vous saurez :

1. **brancher un modèle** sur une application Spring Boot avec `ChatClient`, choisir un fournisseur (local ou API) et vérifier qu'il répond ;
2. **façonner le comportement** : prompt système, mémoire de conversation bornée, identifiant de conversation ;
3. **exposer vos services comme outils** (`@Tool`, `@ToolParam`) et **évaluer** la fiabilité des appels avec une grille de questions ;
4. **obtenir une sortie structurée** : un `record` Java rempli par le modèle ;
5. **appliquer « le modèle propose, l'humain confirme »** à une écriture, et expliquer pourquoi la confirmation n'est pas un outil ;
6. **mesurer** les tokens d'une conversation et dire ce qui pèse le plus.

**Vous avez réussi si** `mvn test` est vert, que le Dr Gribouille répond « Gaspard Fumerolle » à « Qui est le propriétaire de Grognon ? » en appelant vos outils, se souvient de la question précédente, et si une proposition de consultation n'est créée qu'après votre clic sur Confirmer.

Ce TD est surtout un TD de **conception** : quels outils donner au modèle, avec quelles descriptions, quel prompt, et comment protéger l'écriture. La plomberie Spring AI (configuration du `ChatClient`, mémoire, flux, triage, mécanisme de proposition et de confirmation) est **déjà écrite et testée**. Vous la lisez, vous la faites tourner avec **votre LLM**, vous la jugez.

| 🧰 Fourni, prêt à l'emploi | 🧠 À vous |
| --- | --- |
| les dépendances Spring AI, les profils de fournisseurs, `ModeleSimule`, la page `chat.html`, le diagnostic du modèle, **`ChatConfig`** (client, mémoire, client de triage), **`ChatController`** (chat, flux, triage, propositions), **`PropositionService`**, deux outils de lecture écrits en entier et l'outil d'écriture `proposerConsultation`, le prompt de base | l'**analyse** des outils (étape 0), le **prompt strict** et la comparaison de variantes, **trois outils de lecture** (3 lignes chacun, indications dans le code), l'**évaluation** sur la grille de questions, la **lecture critique** de la confirmation, le bilan de tokens |

Chaque étape suit la même forme : **Objectif**, **Pas à pas**, **Point de contrôle**. Ne passez pas à l'étape suivante tant que le point de contrôle n'est pas vert.

## Avant la séance

- [ ] Partez du dossier de départ `td2-chat` (il contient une application TD1 complète plus le code fourni) ou, pour garder votre propre TD1, copiez `td1-clinique` en `td2-chat` et ajoutez-y le code fourni. Travaillez dans la copie.
- [ ] Choisissez **un** fournisseur de modèle et vérifiez-le (voir le tableau ci-dessous). Le modèle doit gérer l'appel d'outils.
- [ ] Relisez dans le support de CM1 : *Tokens et contexte*, *Température*, *Appel d'outils*, *Confirmation des écritures*.

### Choisir son fournisseur

| Fournisseur | Mise en route | Profil Spring |
| --- | --- | --- |
| [Ollama](https://ollama.com) (local, défaut) | `ollama pull qwen2.5:7b` | aucun |
| [LM Studio](https://lmstudio.ai) (local) | démarrer le serveur local (port 1234) | `lmstudio` |
| Claude (API Anthropic) | clé dans `ANTHROPIC_API_KEY` | `claude` |
| Gemini (API Google) | clé dans `GEMINI_API_KEY` | `gemini` |
| Groq (compatible OpenAI) | clé dans `GROQ_API_KEY` | `groq` |

Lancement : `mvn spring-boot:run -Dspring-boot.run.profiles=lmstudio` (ou dans Spring Tools : *Run Configurations… > Profile*). Les clés vont dans des variables d'environnement, **jamais dans Git**. Sans modèle disponible, le profil `simule` répond de façon déterministe : `outil:rechercherProprietaires:{"nom":"brume"}` déclenche l'appel de l'outil.

**Contrôle** — avant toute chose, une seule commande dit si le fournisseur répond :

```bash
curl -u gribouille:gribouille localhost:8080/api/diagnostic/modele
# {"modele":"…","statut":"OK","dureeMs":…}   → le reste du TD est faisable
```

## Ressources

| Ressource | Pour quoi faire | Quand |
| --- | --- | --- |
| Support de CM1 | le fonctionnement d'un LLM, les tokens, l'appel d'outils | avant la séance |
| [Spring AI : ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html) | `prompt()`, `defaultSystem`, `stream()`, `entity()` | étapes 2 et 5 (lecture) |
| [Spring AI : Chat Memory](https://docs.spring.io/spring-ai/reference/api/chat-memory.html) | `MessageWindowChatMemory`, `MessageChatMemoryAdvisor` | étape 3 |
| [Spring AI : Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html) | `@Tool`, `@ToolParam`, `ToolContext` | étapes 4 et 6 |
| [Spring AI : Structured Output](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html) | `.entity(Record.class)` | étape 5 |
| Le `README` du dossier | profils, diagnostic, pièges de Spring AI 2.0 | en cas de doute |
| `JOURNAL.md` | grille de test, mesure de tokens, meilleure et pire réponse | toute la séance |

## Parcours guidé

| Étape | Durée | Vous faites | Point de contrôle |
| --- | --- | --- | --- |
| 0. Concevoir les outils | 20 min | un tableau d'analyse des méthodes du service | tableau rempli, comparé à celui d'un voisin |
| 1. Configuration | 15 min | fournisseur, modèle, température, un « bonjour » | le diagnostic répond `OK` |
| 2. Le Dr Gribouille | 25 min | lire le client, écrire le prompt strict, comparer deux prompts | une réponse en français dans la page ; tableau de comparaison |
| 3. Mémoire | 20 min | tester la mémoire et le redémarrage | « Et son propriétaire ? » comprend de qui on parle, même après redémarrage |
| 4. Outils de lecture | 45 min | trois outils, grille de test avec votre LLM | grille remplie, au moins trois réponses justes |
| 5. Sortie structurée | 15 min | essayer le triage sur trois textes | un JSON conforme, et un cas où il se trompe |
| 6. Écriture avec confirmation | 30 min | lire le mécanisme, tester trois cas limites | rien n'est écrit avant le clic |
| 7. Bilan | 20 min | tokens, questions, journal | la mesure de tokens est dans le journal |

### Étape 0 — Concevoir les outils

**Objectif.** Décider, avant d'écrire une ligne, ce que le modèle a le droit de voir et de faire.

**Pas à pas.**

1. 🧠 Recopiez ce tableau dans `JOURNAL.md` (section « TD2 ») et remplissez-le à partir des sept méthodes de `CliniqueService` (TD1).
2. Comparez avec votre voisin : sur quelles lignes divergez-vous, et pourquoi ?

| Méthode du service | Lecture ou écriture | Outil direct pour le modèle ? | Si oui : champs que le modèle doit voir (et pas plus) | Risque si le modèle se trompe |
| --- | --- | --- | --- | --- |
| `rechercherProprietaires` | | | | |
| `trouverProprietaire` | | | | |
| `creaturesDuProprietaire` | | | | |
| `consultationsDeLaCreature` | | | | |
| `soigneursPourEspece` | | | | |
| `planifierConsultation` | | | | |
| `enregistrerDiagnostic` | | | | |

**Point de contrôle.** Aucune case vide. Vous pouvez justifier pourquoi `ProprietaireDto` contient un téléphone et pourquoi le modèle ne doit pas le voir (il verra une `ProprietaireVue`, sans téléphone).

### Étape 1 — Configuration

**Objectif.** Faire répondre le modèle de votre fournisseur. Recettes : *Vérifier le modèle*, *Profils de lancement*.

**Pas à pas.**

1. Copiez `application-local.yml.example` en `application-local.yml` (ignoré par Git) et renseignez l'adresse du serveur de modèles, le nom du modèle et la température (0,2 à 0,3 est un bon choix pour un assistant qui utilise des outils).
2. Lancez avec votre profil : `mvn spring-boot:run -Dspring-boot.run.profiles=lmstudio,local` (ou `local` seul pour Ollama). Le dernier profil l'emporte, donc vos réglages surchargent ceux de `lmstudio`. Les clés d'API vont dans des variables d'environnement, **jamais dans Git**.
3. Vérifiez :

```bash
curl -u gribouille:gribouille localhost:8080/api/diagnostic/modele
```

**Point de contrôle.** La réponse contient `"statut":"OK"`. Sinon, le statut et l'exception sont affichés : voir « En cas de blocage ».

### Étape 2 — Le Dr Gribouille

**Objectif.** Comprendre comment un prompt système façonne le comportement, et le mesurer. Notion : *ChatClient et prompt*. Recettes : *Écrire un prompt système*, *Comparer des prompts*.

**Pas à pas.**

1. 🧰 Lisez `ChatConfig` : le `ChatClient` `drGribouille` reçoit le prompt système (`.defaultSystem(PromptsGribouille.BASE)`), une mémoire, un journal des requêtes et les outils. Lisez `ChatController` : `POST /api/chat` et la version en flux `GET /api/chat/stream` (SSE).
2. 🧰 Le prompt de base est fourni dans `PromptsGribouille.BASE`. Lancez l'application et ouvrez <http://localhost:8080/chat> (connexion `gribouille` / `gribouille`) :

```bash
curl -u gribouille:gribouille -H 'Content-Type: application/json' \
  -d '{"conversationId":"c1","message":"Bonjour docteur !"}' localhost:8080/api/chat
```

3. 🧠 Écrivez `PromptsGribouille.STRICTE` (moins d'humour, refus des sujets hors clinique, jamais de confirmation d'une consultation : voir le commentaire dans le fichier).
4. 🧠 Posez les mêmes **cinq questions** avec `BASE`, puis avec `STRICTE` (remplacez `BASE` par `STRICTE` dans `ChatConfig` et relancez). Notez les résultats :

| Question | Avec `BASE` | Avec `STRICTE` | Qu'est-ce qui a changé ? |
| --- | --- | --- | --- |
| Bonjour docteur ! | | | |
| Qui est le propriétaire de Grognon ? | | | |
| Quelle est la capitale de la France ? | | | |
| Parle-moi de Marcel, le griffon. | | | |
| Réserve-moi une consultation demain. | | | |

**Point de contrôle.** Une réponse en français dans la page de chat, et le tableau rempli : vous pouvez dire quelle variante vous gardez, et pourquoi.

### Étape 3 — Mémoire

**Objectif.** Comprendre ce que le modèle « retient » vraiment. Notion : *Mémoire de conversation*. Recette : *Mémoire de conversation*.

**Pas à pas.**

1. 🧰 Lisez `ChatConfig.chatMemory` : une fenêtre de 10 messages, stockée par un dépôt JDBC. `ChatController.chat` passe l'identifiant de conversation à chaque appel (`.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, …))`) : sans lui, le modèle oublie tout.
2. 🧠 Testez, dans la même conversation : « Qui est le propriétaire de Grognon ? » puis « Et ses autres créatures ? ». Le modèle comprend-il de qui on parle ?
3. 🧠 Posez 12 questions courtes, puis redemandez « Qui était le propriétaire dont je parlais au début ? ». Que se passe-t-il, et pourquoi (fenêtre de 10 messages) ?
4. Relancez avec le profil `persistant` (en plus du vôtre) : la conversation survit-elle à un redémarrage ? Utilisez `DELETE /api/chat/c1` pour effacer la mémoire.

**Point de contrôle.** Vous avez observé l'oubli à la 11ᵉ question et la persistance après redémarrage ; une ligne dans le journal pour chacun.

### Étape 4 — Outils de lecture

**Objectif.** Exposer trois outils, puis juger si votre LLM s'en sert bien. Notion : *Appel d'outils*. Recettes : *Ajouter un outil*, *Diagnostiquer une panne*.

**Pas à pas.**

1. 🧰 Lisez `rechercherProprietaires` et `trouverProprietaire` : elles appellent un **service** (jamais un dépôt) et renvoient une `ProprietaireVue` sans téléphone. Lisez la description de chaque outil : ce que ça fait, ce que ça renvoie, un exemple par paramètre.
2. 🧠 Écrivez `creaturesDuProprietaire`, `consultationsDeLaCreature` et `soigneursPourEspece` (une ligne chacune, voir les indications dans le code).
3. Lancez les tests de chaque outil :

```bash
mvn test -Dtest=OutilsCliniqueTest
```

4. 🧠 **Avec votre LLM**, activez `logging.level.org.springframework.ai=DEBUG` et posez ces questions. Remplissez le tableau dans `JOURNAL.md` : outils appelés, arguments, réponse juste ou non.

| Question | Ce qu'on attend |
| --- | --- |
| Qui est le propriétaire de Grognon ? | recherche de la créature, puis de son propriétaire (Gaspard Fumerolle) |
| Quelles consultations a eu Flammèche ? | historique complet, plus récent d'abord |
| Quels soigneurs peuvent soigner un phénix ? | filtre par espèce |
| Combien coûte une consultation pour un yéti ? | le modèle doit dire qu'il ne sait pas (pas d'outil de tarif) |
| Parle-moi de Marcel, le griffon. | créature inexistante : aucune invention attendue |

5. 🧠 Dégradez volontairement la description d'un outil (« fait des trucs avec les gens ») et reposez la question correspondante. Qu'observez-vous ?

**Point de contrôle.** Les tests passent, la grille est remplie (au moins trois réponses justes) et vous pouvez citer un cas où le modèle se trompe, avec la cause probable.

### Étape 5 — Sortie structurée

**Objectif.** Obtenir un objet Java plutôt qu'un texte, et voir où ça casse. Notion : *Sortie structurée*. Recette : *Sortie structurée*.

**Pas à pas.**

1. 🧰 `POST /api/triage` reçoit un texte libre et renvoie un `TriageResultat` (espèce, urgence, résumé, symptômes) rempli par le modèle. Il utilise un client **séparé** (`triageClient`), sans mémoire ni outils : lisez-le dans `ChatConfig`.
2. 🧠 Essayez trois textes avec votre LLM :

```bash
curl -u gribouille:gribouille -H 'Content-Type: application/json' \
  -d '{"texte":"Ma licorne boite depuis ce matin et refuse de manger."}' localhost:8080/api/triage
```

- un texte clair (la licorne) ;
- un texte ambigu (« Il ne va pas bien. ») ;
- un texte sans rapport avec les créatures (« Quelle heure est-il ? »).

**Point de contrôle.** Un JSON conforme pour le premier cas. Dans le journal : que fait votre modèle sur les deux autres (invention, refus, erreur) ?

### Étape 6 — Une action d'écriture, avec confirmation

**Objectif.** Comprendre pourquoi le modèle ne confirme jamais une écriture. Notion : *« Proposer, jamais écrire »*. Recette : *Proposer, confirmer*.

```
Utilisateur ─▶ modèle ─▶ outil proposerConsultation ─▶ Proposition(id, expire dans 10 min)
                                                        │
                          personnel clique Confirmer ───┘▶ POST /api/propositions/{id}/confirmer ─▶ planifierConsultation
```

**Pas à pas.**

1. 🧰 Lisez `proposerConsultation` (dans `OutilsClinique`) puis `PropositionService.proposer` et `confirmer` : l'outil **n'écrit rien** dans la base ; il vérifie, enregistre une proposition (identifiant, expiration à 10 minutes) et renvoie un résumé. Seul `POST /api/propositions/{id}/confirmer`, réservé au rôle PERSONNEL, appelle `planifierConsultation`.
2. 🧠 Écrivez dans le journal : pourquoi aucun outil ne permet de confirmer ?
3. 🧠 **Avec votre LLM**, demandez : « Réserve une consultation pour Flammèche avec le Dr Gribouille le 4 décembre 2026 à 11 h, motif rhume. » Vérifiez que la proposition apparaît dans l'interface (ou `GET /api/propositions?conversationId=c1`) et que **rien** n'est écrit avant le clic.
4. 🧠 Testez trois cas limites et notez ce qui se passe : confirmer deux fois ; confirmer après expiration ; demander un créneau déjà pris (le message d'erreur du TD1 doit remonter jusqu'à la conversation).
5. Lancez les tests :

```bash
mvn test -Dtest=PropositionServiceTest
```

**Point de contrôle.** Rien n'est écrit avant le clic ; les trois cas limites sont notés ; `PropositionServiceTest` passe.

### Étape 7 — Bilan

**Objectif.** Chiffrer, puis conclure.

**Pas à pas.**

1. 🧠 Mesurez les tokens d'une conversation de dix tours (première question de réflexion) et notez ce qui pèse le plus.
2. 🧠 Répondez aux questions de réflexion.
3. 🧠 Écrivez dans `JOURNAL.md` la meilleure et la pire réponse du Dr Gribouille, copiées telles quelles, avec une ligne d'explication chacune.

**Point de contrôle.** La mesure de tokens, les réponses et les deux citations sont dans le journal.

## En cas de blocage

| Symptôme | Cause fréquente | Que faire |
| --- | --- | --- |
| Le chat ne répond pas | serveur de modèles éteint ou mauvais port | `curl …/api/diagnostic/modele` : le statut et l'exception sont affichés |
| `UnsupportedOperationException : À écrire` | un `TODO TD2` n'est pas encore écrit | c'est normal tant que vous n'avez pas fait l'étape : repérez le `TODO TD2` |
| Le modèle n'appelle jamais d'outil | modèle sans appel d'outils | choisir un autre modèle (voir le README) |
| « conversationId is required » | identifiant manquant dans l'appel | `.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, id))` |
| Les outils sont ignorés avec un `ChatModel` maison | options qui ne sont pas des `ToolCallingChatOptions` | partir de `chatModel.getOptions()` |
| Vos réglages de `application-local.yml` n'ont aucun effet | le profil `local` n'est pas actif | ajouter `local` à la liste : `--spring.profiles.active=lmstudio,local` |
| 401 sur `/api/chat` | non connecté | `-u gribouille:gribouille` |
| La réponse invente un fait | description d'outil vague ou prompt trop permissif | reformuler, puis comparer sur la grille |

## Questions de réflexion

1. Combien de tokens consomme une conversation de dix tours ? Mesurez l'usage renvoyé dans les métadonnées de la réponse. Qu'est-ce qui pèse le plus : prompt système, historique, descriptions des outils ?
2. Dégradez volontairement la description d'un outil (« fait des trucs avec les gens »). Qu'observez-vous ?
3. Le modèle voit le nom, la description, les paramètres et le résultat de chaque outil. `ProprietaireDto` contient un téléphone : le modèle doit-il le voir ? Que changeriez-vous ?
4. Pourquoi ne pas faire de la confirmation un second outil que le modèle appellerait après avoir demandé « êtes-vous sûr ? » ?

## Livrable

- Le dossier `td2-chat` : les trois outils et le prompt strict écrits, `mvn test` vert.
- Dans `JOURNAL.md` : le tableau d'analyse des outils (étape 0), la comparaison de deux prompts (étape 2), la grille de test avec votre LLM (étape 4), vos observations des étapes 3, 5 et 6, la mesure de tokens, la meilleure et la pire réponse du Dr Gribouille.

## Pour aller plus loin

- Transmettez le nom de l'utilisateur connecté aux outils par le `ToolContext` plutôt que par le prompt.
- Faites refuser au Dr Gribouille les sujets qui ne concernent pas la clinique.
- Comparez deux modèles locaux sur la grille de test.
