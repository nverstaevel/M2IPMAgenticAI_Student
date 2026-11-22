# TD3 — Outils externes et contexte

## Objectifs de la séance

Sortir de la clinique. Le Dr Gribouille gagne des outils qui vont chercher de l'information ou agir à l'extérieur, répartis sur quatre sources : Wikipedia, un fichier Excel, une base de données de médicaments et un agenda. Une expérience chiffrée vous montre ensuite pourquoi il vaut mieux interroger une source que la verser entière dans le contexte.

À la fin de la séance, vous saurez :

1. **écrire un outil** qui interroge une source externe (HTTP, fichier, base, API) et ne renvoie que le strict nécessaire ;
2. **isoler une dépendance externe** derrière une interface (`AgendaPort`) pour tester sans réseau, avec une version réelle et une version simulée ;
3. **appliquer « proposer, jamais écrire »** à un système externe : la confirmation humaine du TD2 devant un agenda ;
4. **mesurer** l'effet de la taille du contexte (tokens, latence, coût, exactitude) et choisir entre « tout dans le prompt » et « outil de recherche » ;
5. **repérer les textes écrits par un tiers** qui entrent dans le contexte du modèle : vous y reviendrez au TD6, quand l'agent lira des notes écrites par des tiers.

**Vous avez réussi si** vos outils (Wikipedia, Excel, médicaments, agenda simulé) passent les tests **et** sont bien choisis et bien utilisés par votre LLM sur la grille de questions, si `mesures.csv` contient 20 lignes (10 questions × 2 stratégies) et si vous pouvez dire, chiffres à l'appui, à partir de quelle taille la stratégie A n'est plus raisonnable.

Ce TD est surtout un TD de **conception**. La plomberie technique (appels HTTP, lecture du classeur, requête SQL, agenda en mémoire, chronomètre de l'expérience) est **déjà écrite et testée** dans le dossier de départ. Vous décidez ce que le modèle voit et ce qu'il peut faire.

| 🧰 Fourni, prêt à l'emploi | 🧠 À vous |
| --- | --- |
| `WikipediaClient`, `TarifsExcelReader`, `CatalogueMedicaments` (requête SQL paramétrée), `AgendaPort`, `AgendaSimule` et `GoogleAgendaAdapter`, `ExperienceContexte` (stratégies A et B, mesures) et son lanceur `ExperienceController`, les fichiers de données, **l'outil `chercherDansWikipedia` en entier (votre modèle)** | l'**analyse** des sources (étape 0), les **5 autres outils** de `OutilsExternes` (court : 5 à 15 lignes chacun, avec des indications dans le code), la **lecture critique** de l'expérience, la **synthèse chiffrée** |

Chaque étape suit la même forme : **Objectif**, **Pas à pas**, **Point de contrôle**. Ne passez pas à l'étape suivante tant que le point de contrôle n'est pas vert.

## Avant la séance

- [ ] Partez du dossier de départ `td3-outils-externes` (il contient votre TD2 corrigé plus le code fourni) ou, pour garder votre propre TD2, copiez `td2-chat` en `td3-outils-externes` et ajoutez-y le code fourni. Travaillez dans la copie.
- [ ] Fichiers fournis : `tarifs-et-pharmacie.xlsx` (feuilles Tarifs et Pharmacie), le script de la base `catalogue-medicaments`, `grand-bestiaire.txt` (un gros corpus), `questions-reference.csv` (questions et réponses attendues).
- [ ] Contrôlez le modèle : `curl -u gribouille:gribouille localhost:8080/api/diagnostic/modele` doit répondre `"statut":"OK"`.
- [ ] Relisez dans le support de CM2 : *Ports et adaptateurs*, *Sources externes*, *Interroger plutôt que charger*.

## Ressources

| Ressource | Pour quoi faire | Quand |
| --- | --- | --- |
| Support de CM2 | le principe port/adaptateur, l'expérience de contexte | avant la séance |
| [API REST de Wikimedia](https://www.mediawiki.org/wiki/API:REST_API) et [politique de User-Agent](https://foundation.wikimedia.org/wiki/Policy:User-Agent_policy) | l'URL du résumé d'une page et l'en-tête obligatoire | étape 1 |
| [Apache POI : guide rapide du tableur](https://poi.apache.org/components/spreadsheet/quick-guide.html) | lire un classeur `.xlsx` | étape 2 |
| [Spring Boot : configurer deux sources de données](https://docs.spring.io/spring-boot/how-to/data-access.html) | une seconde base avec son `@Qualifier` | étape 3 |
| [API Google Calendar : démarrage Java](https://developers.google.com/calendar/api/quickstart/java) | le client et l'authentification | « Pour aller plus loin » |
| [Lost in the Middle](https://arxiv.org/abs/2307.03172) (Liu et al., 2023) | pourquoi la position d'un passage dans le contexte compte | étape 5, question 2 |
| [Spring AI : Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html) | `@Tool`, `@ToolParam`, descriptions | toute la séance |

## Parcours guidé

| Étape | Durée | Vous faites | Point de contrôle |
| --- | --- | --- | --- |
| 0. Concevoir avant de coder | 20 min | un tableau d'analyse des quatre sources dans `JOURNAL.md` | tableau rempli, relu par votre voisin |
| 1. Wikipedia | 10 min | lire l'outil modèle, l'essayer | « Parle-moi du phénix » cite la page ; une page absente donne un message clair |
| 2. Excel | 20 min | `consulterTarif`, `stockPharmacie`, essai avec votre LLM | tests tarif et stock verts ; le LLM appelle l'outil |
| 3. Base externe | 20 min | `chercherMedicament(nom, espece)`, essai avec votre LLM | tests verts ; le LLM n'invente pas de dose |
| 4. Agenda | 30 min | `creneauxLibres`, `proposerEvenementAgenda`, essai avec votre LLM | 7 tests verts ; le LLM propose sans réserver |
| 5. Expérience sur le contexte | 30 min | lancer l'expérience, lire `mesures.csv` | 20 lignes ; vous savez dire où la stratégie A casse |
| 6. Synthèse | 20 min | tableau, conclusions, journal | cinq lignes de conclusion chiffrées |

Le socle représente environ 2 h 30. Le reste est pour la section « Pour aller plus loin ».

### Étape 0 — Concevoir avant de coder

**Objectif.** Décider, sur papier, ce que chaque source apporte au Dr Gribouille et ce qu'elle risque de lui faire dire.

**Pas à pas.**

1. Ouvrez `JOURNAL.md` et recopiez le tableau ci-dessous sous un titre « TD3 ».
2. Remplissez-le **sans regarder le code**, avec ce que vous savez des sources : Wikipedia (texte libre, sur Internet), le classeur Excel (tarifs et stock), la base de médicaments (fiches avec contre-indications), l'agenda (créneaux).
3. Comparez avec votre voisin : sur quelles lignes n'êtes-vous pas d'accord ?

| Source | Question typique d'un propriétaire | Outil (nom) | Lecture ou écriture ? | Ce qu'il doit renvoyer (et pas plus) | Si la source échoue, il répond… | Qui a écrit le texte renvoyé ? |
| --- | --- | --- | --- | --- | --- | --- |
| Wikipedia | « Que mange un phénix ? » | `chercherDansWikipedia` | | | | |
| Excel | « Combien coûte un vaccin pour un yéti ? » | | | | | |
| Base de médicaments | « Le baume ignifuge est-il sûr pour mon dragon ? » | | | | | |
| Agenda | « Avez-vous un créneau jeudi ? » | | | | | |

**Point de contrôle.** Aucune case vide, et la colonne « Qui a écrit le texte renvoyé ? » distingue clairement ce que la clinique maîtrise de ce qui vient d'un tiers. Vous la compléterez à l'étape 6.

### Étape 1 — Wikipedia

**Objectif.** Lire un outil complet pour en tirer le motif que vous recopierez cinq fois.

**Pas à pas.**

1. 🧰 Ouvrez `OutilsExternes.java` : `chercherDansWikipedia` est écrit. Repérez les quatre temps : appeler la source, mettre en forme, **toujours renvoyer du texte** (même en cas d'échec), **étiqueter** la source non fiable.
2. 🧰 `WikipediaClient` est fourni : il envoie le `User-Agent` exigé par Wikimedia, tronque à 1 500 caractères et signale pages absentes et pages d'homonymie. Ne le modifiez pas.
3. 🧠 Lisez la description de l'outil (`@Tool(description = …)`). Relevez dans votre journal : à quel moment le modèle doit-il l'appeler ? À quel moment ne le doit-il pas ?
4. Lancez l'application avec le profil `simule` (`mvn spring-boot:run -Dspring-boot.run.profiles=simule`) et appelez l'outil sans modèle :

```bash
curl -u gribouille:gribouille -H 'Content-Type: application/json' \
  -d '{"conversationId":"c1","message":"outil:chercherDansWikipedia:{\"titre\":\"Phénix (mythologie)\"}"}' localhost:8080/api/chat
```

**Point de contrôle.** La réponse commence par « [Source externe non fiable : Wikipédia … ». Avec le titre `Zzzzzz`, vous obtenez « Aucune page Wikipédia trouvée ». **Avec votre LLM** (profil `lmstudio`, etc.), « Parle-moi du phénix » doit appeler l'outil et citer la page, sans traiter le texte reçu comme une consigne ; notez-le dans le tableau « Essais avec le LLM » de `JOURNAL.md`.

### Étape 2 — Excel

**Objectif.** Exposer deux outils à partir d'un lecteur de classeur déjà prêt. Recette : *Lire un Excel*.

**Pas à pas.**

1. 🧰 `TarifsExcelReader` lit le classeur **une seule fois au démarrage** : `tarif(acte, espece)` renvoie un `Optional<Double>`, `stock(medicament)` un `Optional<Integer>`, `unite(medicament)` un texte.
2. 🧠 Dans `consulterTarif`, suivez les indications écrites dans le code (bloc « À VOUS 1 ») : mettez en forme le prix en euros avec une virgule, et, si l'acte est inconnu, **listez les actes connus** pour que le modèle puisse se corriger.
3. 🧠 Dans `stockPharmacie` (« À VOUS 2 ») : renvoyez la quantité et l'unité, et ajoutez « (rupture) » quand le stock est à zéro.
4. Lancez les deux tests concernés :

```bash
mvn test -Dtest=OutilsExternesTest#consulterTarifLitLeClasseurExcel+stockPharmacie
```

🧠 **Essayez avec votre LLM** (profil `lmstudio`, `groq`, etc., pas `simule`) : les tests passent, mais le modèle choisit-il vos outils ? Posez ces questions au chat, avec `logging.level.org.springframework.ai=DEBUG` pour voir les appels d'outils dans la console :

- « Combien coûte une consultation pour un yéti ? » → doit appeler `consulterTarif`
- « Reste-t-il de la poudre de lune ? » → doit appeler `stockPharmacie`

```bash
curl -u gribouille:gribouille -H 'Content-Type: application/json' \
  -d '{"conversationId":"td3-t2","message":"Combien coûte une consultation pour un yéti ?"}' localhost:8080/api/chat
```

Notez dans le tableau « Essais avec le LLM » de `JOURNAL.md` : l'outil appelé, les arguments, et si la réponse est juste. Si le modèle n'appelle pas l'outil ou invente un prix, la cause est le plus souvent la description : reformulez-la et réessayez.

**Point de contrôle.** Les deux tests passent **et** le LLM répond avec le bon tarif en appelant l'outil. Le tarif d'une consultation pour un dragon est bien 80,00 €. Question à vous poser : pourquoi renvoyer une phrase plutôt que la ligne brute du classeur ?

### Étape 3 — Base externe

**Objectif.** Interroger une seconde base **sans jamais laisser le modèle écrire du SQL**. Recette : *SQL paramétré*.

**Pas à pas.**

1. 🧰 `CatalogueMedicaments.chercher(nom, espece)` contient la requête : le modèle ne fournit que deux valeurs, liées comme paramètres. Lisez-la : elle est la raison pour laquelle l'injection SQL échoue.
2. 🧠 Dans `chercherMedicament` (« À VOUS 3 »), construisez la réponse : une ligne par fiche, avec les **contre-indications bien visibles**. Liste vide : un message clair.
3. Lancez les tests :

```bash
mvn test -Dtest=OutilsExternesTest#chercherMedicamentInterrogeLaSecondeBase+chercherMedicamentResisteAUneInjectionSql
```

🧠 **Essayez avec votre LLM** (profil `lmstudio`, `groq`, etc., pas `simule`) : les tests passent, mais le modèle choisit-il vos outils ? Posez ces questions au chat, avec `logging.level.org.springframework.ai=DEBUG` pour voir les appels d'outils dans la console :

- « Le baume ignifuge est-il sûr pour un dragon ? » → doit appeler `chercherMedicament`
- « Donne-moi une dose de baume pour un vampire. » → doit appeler chercherMedicament (réponse attendue : aucun médicament au catalogue, sans que le modèle invente une dose)

```bash
curl -u gribouille:gribouille -H 'Content-Type: application/json' \
  -d '{"conversationId":"td3-t3","message":"Le baume ignifuge est-il sûr pour un dragon ?"}' localhost:8080/api/chat
```

Notez dans le tableau « Essais avec le LLM » de `JOURNAL.md` : l'outil appelé, les arguments, et si la réponse est juste. Le second cas teste ce que fait le modèle quand l'outil répond « rien » : inventer est un échec.

**Point de contrôle.** Les deux tests passent. Dans votre journal : si le modèle pouvait écrire du SQL, que pourrait-il faire à partir d'un message de propriétaire ?

### Étape 4 — Agenda

**Objectif.** Appliquer « proposer, jamais écrire » à un système externe. Recette : *Proposer, confirmer*.

**Pas à pas.**

1. 🧰 L'agenda est caché derrière `AgendaPort`. `AgendaSimule` (en mémoire) est fourni et choisi par défaut : les tests ne dépendent ni du réseau ni d'un compte. Il démarre avec deux créneaux occupés, le **3 décembre 2026 à 10 h et à 14 h 30**.
2. 🧠 Dans `creneauxLibres` (« À VOUS 4 »), transformez la date, appelez l'agenda, formatez la liste. Une date illisible donne un message, pas une exception.
3. 🧠 Dans `proposerEvenementAgenda` (« À VOUS 5 »), appelez `propositions.proposer(...)`. **Rien n'est créé dans l'agenda** à ce stade ; la confirmation par un membre du personnel (code fourni, comme au TD2) crée l'événement « Consultation — Flammèche (Dragon) » **sans invité**.
4. Lancez les tests :

```bash
mvn test -Dtest=OutilsExternesTest
```

🧠 **Essayez avec votre LLM** (profil `lmstudio`, `groq`, etc., pas `simule`) : les tests passent, mais le modèle choisit-il vos outils ? Posez ces questions au chat, avec `logging.level.org.springframework.ai=DEBUG` pour voir les appels d'outils dans la console :

- « Avez-vous un créneau le 3 décembre 2026 ? » → doit appeler `creneauxLibres`
- « Réserve le 4 décembre 2026 à 11 h une consultation pour Flammèche, motif rhume. » → doit appeler proposerEvenementAgenda (puis vérifiez que l'agenda n'a PAS été modifié et que la proposition attend confirmation)

```bash
curl -u gribouille:gribouille -H 'Content-Type: application/json' \
  -d '{"conversationId":"td3-t4","message":"Avez-vous un créneau le 3 décembre 2026 ?"}' localhost:8080/api/chat
```

Notez dans le tableau « Essais avec le LLM » de `JOURNAL.md` : l'outil appelé, les arguments, et si la réponse est juste. Le modèle doit annoncer une **proposition**, jamais une réservation faite.

**Point de contrôle.** Tous les tests de `OutilsExternesTest` passent (7 sur 7) et les essais avec le LLM sont notés dans le journal. Question de conception, à écrire dans le journal : une consultation existe maintenant dans la base et dans l'agenda. Laquelle fait foi ? Que se passe-t-il quand elles divergent ?

### Étape 5 — Expérience sur le contexte

**Objectif.** Mesurer pourquoi on interroge une source plutôt que de la verser entière dans le prompt.

Le corpus `grand-bestiaire.txt` est trop gros pour être lu en entier avec confort. Chacune des dix questions de `questions-reference.csv` a sa réponse dans un court passage caché quelque part. Deux stratégies sont comparées :

| Stratégie | Principe |
| --- | --- |
| A. Tout dans le prompt | Le corpus (tronqué à `tailleMax` caractères) est placé dans le prompt. |
| B. Recherche | `chercherDansBestiaire(motsCles)` renvoie au plus 3 passages de 800 caractères. |

**Pas à pas.**

1. 🧰 Tout le code de mesure est fourni (`ExperienceContexte` : chronomètre, jetons estimés à 1 pour 4 caractères, coût fictif, exactitude 0 ou 1). Lancez l'expérience avec un **vrai modèle** (profil `lmstudio`, `groq`, etc. ; pas `simule`) :

```bash
curl -u gribouille:gribouille -X POST "localhost:8080/api/experience/lancer?tailleMax=5000"
```

2. Ouvrez `mesures.csv` (à la racine du projet, séparateur `;`). Il contient 20 lignes : 10 questions × 2 stratégies.
3. Relancez avec `tailleMax=20000`, puis `60000`. **Gardez chaque fichier** (renommez-le `mesures-5000.csv`, etc.).
4. 🧠 Pour chaque lancement, calculez par stratégie : jetons moyens, latence moyenne, coût total, nombre de réponses exactes.

```csv
question;strategie;position;bruit;jetons_entree;jetons_sortie;latence_ms;cout_estime;exactitude
Q1;A;-;-;1250;35;6120;0.002710;0
Q1;B;-;-;460;42;1310;0.001172;1
```

**Point de contrôle.** Trois fichiers de 20 lignes. Vous savez répondre, chiffres à l'appui : à partir de quelle taille la stratégie A n'est plus raisonnable, et sur quel critère d'abord (erreur, coût, latence ou exactitude).

### Étape 6 — Synthèse

**Objectif.** Transformer des chiffres en décisions.

**Pas à pas.**

1. 🧠 Dans `JOURNAL.md`, remplissez le tableau de synthèse (une ligne par stratégie et par `tailleMax` : jetons moyens, latence moyenne, coût, exactitude).
2. 🧠 Écrivez **cinq lignes de conclusion**, chacune avec un chiffre. Modèle : « À 20 000 caractères, la stratégie A coûte trois fois plus que B pour une exactitude de 6/10 contre 9/10. »
3. 🧠 Revenez au tableau de l'étape 0 : corrigez ce que l'expérience ou le code vous a appris, et complétez la dernière colonne.

**Point de contrôle.** Cinq conclusions chiffrées, un tableau d'étape 0 corrigé. Rendez le dossier et le journal (voir « Livrable »).

## En cas de blocage

| Symptôme | Cause fréquente | Que faire |
| --- | --- | --- |
| Wikipedia répond 403 | pas de `User-Agent` | ajouter un en-tête qui nomme votre application et un contact |
| `UnsupportedOperationException : À écrire` | un outil n'est pas encore écrit | c'est normal tant que vous n'avez pas fait l'étape : repérez le `TODO TD3` correspondant |
| Le test échoue sur un texte (« expected … to contain … ») | votre phrase ne contient pas le mot attendu | lisez le test : il dit ce que la réponse doit contenir (« 80,00 », « rupture », « illisible »…) |
| Exactitude à 0 partout, quelle que soit la stratégie | profil `simule` : le modèle simulé ne lit pas le contexte | relancer avec un vrai modèle (`lmstudio`, `groq`…) |
| « context length exceeded » en stratégie A | corpus plus grand que la fenêtre du modèle | c'est un résultat : notez la taille qui fait échouer |
| Le modèle n'appelle pas l'outil, ou l'appelle avec un argument inventé | description trop vague, ou modèle sans appel d'outils | reformuler la description, ajouter un exemple de valeur dans `@ToolParam` ; vérifier avec les logs `DEBUG` ; essayer un autre modèle |

## Questions de réflexion

1. À partir de quelle taille de corpus la stratégie A échoue-t-elle, et dans quel ordre : erreur, coût, latence, exactitude ?
2. La position du passage utile change-t-elle l'exactitude ? Qu'en déduisez-vous pour la façon d'organiser un prompt ?
3. Que doit renvoyer un outil : tout, ou le strict nécessaire ? Trouvez un cas où en renvoyer moins fait perdre une information utile.
4. Parmi vos quatre outils, lesquels renvoient du texte écrit par un tiers ? Que pourrait contenir ce texte ? Gardez la réponse : le TD6 y reviendra.
5. Votre assistant sait maintenant écrire dans l'agenda. Quelles protections avez-vous mises, et lesquelles manquent encore ?

## Livrable

- Le dossier `td3-outils-externes` : les six outils de `OutilsExternes` qui fonctionnent, `OutilsExternesTest` entièrement vert.
- Dans `JOURNAL.md` : le tableau d'analyse (étape 0, corrigé), le tableau de synthèse de l'expérience et vos cinq lignes de conclusion chiffrées.
- Les fichiers `mesures-*.csv` de l'étape 5.

## Extensions

Si le socle est terminé avant la fin de la séance :

- faites varier la position du passage utile (début, milieu, fin) et la quantité de texte parasite (aucune, moitié, double) avec la stratégie A (`ExperienceContexte.avecVariation` est fourni) et notez les résultats dans le journal ;
- implémentez la stratégie C (similarité, magasin vectoriel en mémoire) et comparez-la à B.

## Pour aller plus loin

- Mettez en cache les pages Wikipedia déjà lues.
- Faites la stratégie C avec un vrai modèle d'embeddings et comparez-la à B.
- Ajoutez un outil `comparerTarifs(espece1, espece2)` et observez si le modèle l'utilise à bon escient.
- **Brancher le vrai Google Agenda** (facultatif, demande un compte Google personnel). `GoogleAgendaAdapter` est fourni. Dans Google Cloud (gratuit), créez un projet, activez l'API Google Calendar, créez un compte de service et générez une clé JSON (le fichier ne va jamais dans Git). Dans Google Agenda, créez un agenda de test vide et partagez-le avec l'adresse du compte de service (`…@….iam.gserviceaccount.com`) avec le droit « Apporter des modifications aux événements ». Puis renseignez `agenda.mode=google`, `agenda.google.calendar-id` (identifiant de l'agenda) et `agenda.google.credentials-file` (chemin de la clé) dans `application-local.yml`. Si Google répond par une erreur d'authentification, vérifiez le partage et l'identifiant ; en cas de doute, revenez à `agenda.mode=simule`.
