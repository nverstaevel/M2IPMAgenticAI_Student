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

**Vous avez réussi si** le Dr Gribouille répond « Gaspard Fumerolle » à « Qui est le propriétaire de Grognon ? » en appelant vos outils, se souvient de la question précédente, et si une proposition de consultation n'est créée qu'après votre clic sur Confirmer.

| Vous écrivez | Fourni dans le dossier de départ |
| --- | --- |
| la configuration du `ChatClient`, le prompt système, les outils, le triage, la proposition et sa confirmation | les dépendances Spring AI, les profils de fournisseurs, `ModeleSimule` pour les tests, la page `chat.html`, le diagnostic du modèle |

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
| [Spring AI : ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html) | `prompt()`, `defaultSystem`, `stream()`, `entity()` | étapes 2 et 5 |
| [Spring AI : Chat Memory](https://docs.spring.io/spring-ai/reference/api/chat-memory.html) | `MessageWindowChatMemory`, `MessageChatMemoryAdvisor` | étape 3 |
| [Spring AI : Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html) | `@Tool`, `@ToolParam`, `ToolContext` | étapes 4 et 6 |
| [Spring AI : Structured Output](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html) | `.entity(Record.class)` | étape 5 |
| Le `README` du dossier | profils, diagnostic, pièges de Spring AI 2.0 | en cas de doute |
| `JOURNAL.md` | grille de test, mesure de tokens, meilleure et pire réponse | toute la séance |

## Parcours guidé

| Étape | Durée | Vous faites | Point de contrôle |
| --- | --- | --- | --- |
| 1. Configuration | 15 min | fournisseur, modèle, température, un « bonjour » | le diagnostic répond `OK` |
| 2. Le Dr Gribouille | 25 min | `ChatClient`, `/api/chat`, flux SSE, page de chat | une réponse en français, avec humour, dans la page |
| 3. Mémoire | 25 min | fenêtre de 10 messages, dépôt JDBC | « Et son propriétaire ? » comprend de qui on parle, même après redémarrage |
| 4. Outils de lecture | 40 min | cinq outils, grille de test | la grille est remplie, au moins trois réponses justes |
| 5. Sortie structurée | 20 min | `POST /api/triage` | un JSON conforme à votre `record` |
| 6. Écriture avec confirmation | 35 min | proposition, bouton Confirmer | rien n'est écrit avant le clic |
| 7. Bilan | 20 min | tokens, questions, journal | la mesure de tokens est dans le journal |

### Étape 1 — Configuration

Renseignez dans `application-local.yml` (copié depuis `application-local.yml.example`, ignoré par Git) l'adresse du serveur de modèles, le nom du modèle et la température. Ce fichier n'est lu que si le profil `local` est actif : lancez avec `-Dspring-boot.run.profiles=local` (Ollama) ou `lmstudio,local` (le dernier profil l'emporte, donc vos réglages surchargent ceux de `lmstudio`). Faites dire bonjour au modèle depuis un `CommandLineRunner`, puis supprimez-le.

### Étape 2 — Le Dr Gribouille

Créez un `ChatClient` avec un prompt système. Exposez `POST /api/chat` (`conversationId`, `message`) et une version en flux `GET /api/chat/stream` (`.stream().content()`, renvoyé en SSE). Ajoutez une page de chat Thymeleaf minimale. Le prompt système est à vous ; voici une base à déformer en trois variantes et à comparer :

> Tu es le Dr Gribouille, vétérinaire de la clinique des créatures fantastiques. Tu réponds en français, avec humour mais sans jamais inventer un fait du dossier. Pour toute information sur un propriétaire, une créature ou une consultation, tu utilises tes outils. Si un outil ne répond pas, tu le dis.

```bash
curl -u gribouille:gribouille -H 'Content-Type: application/json' \
  -d '{"conversationId":"c1","message":"Bonjour docteur !"}' localhost:8080/api/chat
```

### Étape 3 — Mémoire

Ajoutez un `MessageChatMemoryAdvisor` basé sur `MessageWindowChatMemory`, fenêtre de 10 messages. Passez l'identifiant de conversation à chaque appel : il est obligatoire. Passez ensuite à un dépôt JDBC pour que la conversation survive à un redémarrage (profil `persistant`).

```java
ChatClient chat = builder
    .defaultSystem(PROMPT_GRIBOUILLE)
    .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), new SimpleLoggerAdvisor())
    .defaultTools(outilsClinique)
    .build();

chat.prompt().user(message)
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
    .call().content();
```

Activez le journal de débogage (`logging.level.org.springframework.ai=DEBUG`) pour voir les requêtes et les appels d'outils.

### Étape 4 — Outils de lecture

Exposez comme outils les cinq méthodes de **lecture** du TD1 (`rechercherProprietaires`, `trouverProprietaire`, `creaturesDuProprietaire`, `consultationsDeLaCreature`, `soigneursPourEspece`) avec `@Tool` et `@ToolParam`, branchés avec `.defaultTools(...)`. Les deux méthodes d'écriture ne deviennent **pas** des outils directs (étape 6). Un outil se résume à une méthode annotée (extrait indicatif) ; **la description est ce que le modèle lit pour choisir** :

```java
@Component
class OutilsClinique {
    private final CliniqueService service;

    @Tool(description = "Cherche les propriétaires dont le nom contient le texte donné. Renvoie id, nom, prénom et ville.")
    List<ProprietaireDto> rechercherProprietaires(
            @ToolParam(description = "Partie du nom de famille") String nom) {
        return service.rechercherProprietaires(nom);
    }
}
```

Posez ces questions et remplissez le tableau dans `JOURNAL.md` : outils appelés, arguments, réponse juste ou non.

| Question | Ce qu'on attend |
| --- | --- |
| Qui est le propriétaire de Grognon ? | recherche de la créature, puis de son propriétaire (Gaspard Fumerolle) |
| Quelles consultations a eu Flammèche ? | historique complet, plus récent d'abord |
| Quels soigneurs peuvent soigner un phénix ? | filtre par espèce |
| Combien coûte une consultation pour un yéti ? | le modèle doit dire qu'il ne sait pas (pas d'outil de tarif) |
| Parle-moi de Marcel, le griffon. | créature inexistante : aucune invention attendue |

### Étape 5 — Sortie structurée

`POST /api/triage` reçoit un texte libre et renvoie un `record` Java rempli par le modèle. Utilisez un client **séparé**, sans mémoire ni outils.

```java
record Triage(String espece, String symptome, Urgence urgence) {}
enum Urgence { FAIBLE, MOYENNE, ELEVEE }

Triage t = clientTriage.prompt()
    .user("Ma licorne boite depuis ce matin et refuse de manger.")
    .call().entity(Triage.class);
```

### Étape 6 — Une action d'écriture, avec confirmation

1. Un outil `proposerConsultation(creatureId, soigneurId, dateHeure, motif)` **n'écrit rien** dans la base. Il vérifie les données, enregistre une `PropositionConsultation` (identifiant, expiration à 10 minutes) et renvoie un résumé.
2. L'interface affiche la proposition avec deux boutons. Le bouton Confirmer appelle `POST /api/propositions/{id}/confirmer`, réservé au rôle PERSONNEL, qui appelle `planifierConsultation`.
3. Aucun outil ne permet au modèle de confirmer. C'est volontaire : la décision d'écrire ne doit pas dépendre de ce que le modèle « croit » avoir compris.
4. Testez le cas du conflit de créneau : le message d'erreur du TD1 doit remonter jusqu'à la conversation.

```
Utilisateur ─▶ modèle ─▶ outil proposerConsultation ─▶ Proposition(id, expire dans 10 min)
                                                        │
                          personnel clique Confirmer ───┘▶ POST /api/propositions/{id}/confirmer ─▶ planifierConsultation
```

### Étape 7 — Bilan

Complétez la grille, mesurez les tokens (voir la première question de réflexion), répondez aux questions, écrivez dans `JOURNAL.md` la meilleure et la pire réponse du Dr Gribouille, copiées telles quelles.

## En cas de blocage

| Symptôme | Cause fréquente | Que faire |
| --- | --- | --- |
| Le chat ne répond pas | serveur de modèles éteint ou mauvais port | `curl …/api/diagnostic/modele` : le statut et l'exception sont affichés |
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

- Le dossier `td2-chat` : chat en flux, mémoire persistée, cinq outils de lecture, endpoint de triage, proposition et confirmation de consultation.
- Un test unitaire par outil, qui appelle la méthode directement, sans modèle.
- Dans `JOURNAL.md` : la grille de test remplie, la mesure de tokens, la meilleure et la pire réponse du Dr Gribouille.

## Pour aller plus loin

- Transmettez le nom de l'utilisateur connecté aux outils par le `ToolContext` plutôt que par le prompt.
- Faites refuser au Dr Gribouille les sujets qui ne concernent pas la clinique.
- Comparez deux modèles locaux sur la grille de test.
