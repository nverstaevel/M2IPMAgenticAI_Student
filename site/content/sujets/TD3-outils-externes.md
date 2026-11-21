# TD3 — Outils externes et contexte

## Objectifs de la séance

Sortir de la clinique. Le Dr Gribouille gagne des outils qui vont chercher de l'information ou agir à l'extérieur, répartis sur quatre sources : Wikipedia, un fichier Excel, une base de données de médicaments et un agenda. Une expérience chiffrée vous montre ensuite pourquoi il vaut mieux interroger une source que la verser entière dans le contexte.

À la fin de la séance, vous saurez :

1. **écrire un outil** qui interroge une source externe (HTTP, fichier, base, API) et ne renvoie que le strict nécessaire ;
2. **isoler une dépendance externe** derrière une interface (`AgendaPort`) pour tester sans réseau, avec une version réelle et une version simulée ;
3. **appliquer « proposer, jamais écrire »** à un système externe : la confirmation humaine du TD2 devant Google Agenda ;
4. **mesurer** l'effet de la taille du contexte (tokens, latence, coût, exactitude) et choisir entre « tout dans le prompt » et « outil de recherche » ;
5. **repérer les textes écrits par un tiers** qui entrent dans le contexte du modèle : vous y reviendrez au TD6, quand l'agent lira des notes écrites par des tiers.

**Vous avez réussi si** vos outils (Wikipedia, Excel, médicaments, agenda simulé) répondent correctement à la grille de questions, si `mesures.csv` contient 20 lignes (10 questions × 2 stratégies) et si vous pouvez dire, chiffres à l'appui, à partir de quelle taille la stratégie A n'est plus raisonnable.

| Vous écrivez | Fourni dans le dossier de départ |
| --- | --- |
| les outils de `OutilsExternes` (Wikipédia, tarifs et stock Excel, médicaments, agenda), `WikipediaClient`, `AgendaSimule`, la fonction `mesurer` d'`ExperienceContexte` | `TarifsExcelReader` (lecture du classeur), `AgendaPort` et `GoogleAgendaAdapter` (client Google Agenda réel), la seconde source de données (`CatalogueConfig`), `ExperienceContexte` (lit le CSV, écrit les mesures), les fichiers de données ci-dessous |

## Avant la séance

- [ ] Partez du dossier de départ `td3-outils-externes` (il contient votre TD2 corrigé plus le code fourni) ou, pour garder votre propre TD2, copiez `td2-chat` en `td3-outils-externes` et ajoutez-y le code fourni. Travaillez dans la copie.
- [ ] Fichiers fournis : `tarifs-et-pharmacie.xlsx` (feuilles Tarifs et Pharmacie), le script de la base `catalogue-medicaments`, `grand-bestiaire.txt` (un gros corpus), `questions-reference.csv` (questions et réponses attendues), et, pour l'extension Google Agenda seulement, l'adresse du compte de service (dans le `README` du dossier `td3-outils-externes`) et son fichier de clés, distribué en séance : il ne va jamais dans Git.
- [ ] Contrôlez le modèle : `curl -u gribouille:gribouille localhost:8080/api/diagnostic/modele` doit répondre `"statut":"OK"`.
- [ ] Relisez dans le support de CM2 : *Ports et adaptateurs*, *Sources externes*, *Interroger plutôt que charger*.

## Ressources

| Ressource | Pour quoi faire | Quand |
| --- | --- | --- |
| Support de CM2 | le principe port/adaptateur, l'expérience de contexte | avant la séance |
| [API REST de Wikimedia](https://www.mediawiki.org/wiki/API:REST_API) et [politique de User-Agent](https://foundation.wikimedia.org/wiki/Policy:User-Agent_policy) | l'URL du résumé d'une page et l'en-tête obligatoire | étape 1 |
| [Apache POI : guide rapide du tableur](https://poi.apache.org/components/spreadsheet/quick-guide.html) | lire un classeur `.xlsx` | étape 2 |
| [Spring Boot : configurer deux sources de données](https://docs.spring.io/spring-boot/how-to/data-access.html) | une seconde base avec son `@Qualifier` | étape 3 |
| [API Google Calendar : démarrage Java](https://developers.google.com/calendar/api/quickstart/java) | le client et l'authentification | étape 4 |
| [Lost in the Middle](https://arxiv.org/abs/2307.03172) (Liu et al., 2023) | pourquoi la position d'un passage dans le contexte compte | étape 5, question 2 |
| [Spring AI : Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html) | `@Tool`, `@ToolParam`, descriptions | toute la séance |

## Parcours guidé

| Étape | Durée | Vous faites | Point de contrôle |
| --- | --- | --- | --- |
| 1. Wikipedia | 20 min | `chercherDansWikipedia(titre)` et `WikipediaClient` | « Parle-moi du phénix » cite la page ; une page absente donne un message clair |
| 2. Excel | 10 min | `consulterTarif`, `stockPharmacie` (le lecteur du classeur est fourni) | le tarif d'une consultation pour un yeti est celui du classeur |
| 3. Base externe | 15 min | `chercherMedicament(nom, espece)` (la seconde source de données est fournie) | les contre-indications s'affichent, jamais de SQL écrit par le modèle |
| 4. Google Agenda | 25 min | `AgendaSimule` et deux outils (`AgendaPort` et l'adaptateur Google sont fournis) | créneaux libres avec l'agenda simulé ; proposition puis confirmation |
| 5. Expérience sur le contexte | 35 min | `mesurer`, stratégies A et B sur les dix questions | `mesures.csv` rempli (20 lignes) |
| 6. Synthèse | 20 min | tableau, conclusions, journal | cinq lignes de conclusion chiffrées |

Le socle représente environ 2 h 05. Le reste de la séance est pour les extensions (section « Extensions »).

### Étape 1 — Wikipedia

Complétez le squelette et exposez `chercherDansWikipedia(titre)`. Il renvoie le résumé de la page en français, tronqué à 1 500 caractères. Gérez les pages absentes et les pages d'homônymie par un message clair. Envoyez un en-tête `User-Agent` qui identifie votre application, comme l'exige Wikimedia.

```java
@Tool(description = "Résumé d'une page Wikipedia en français. À utiliser pour le folklore d'une créature.")
String chercherDansWikipedia(@ToolParam(description = "Titre exact de la page, ex. Phénix (mythologie)") String titre) {
    return wikipedia.resume(titre)                        // GET https://fr.wikipedia.org/api/rest_v1/page/summary/{titre}
                    .map(t -> t.length() > 1500 ? t.substring(0, 1500) : t)
                    .orElse("Aucune page Wikipedia ne porte ce titre.");
}
```

### Étape 2 — Excel

Exposez `consulterTarif(acte, espece)` et `stockPharmacie(medicament)`. Lisez le classeur **une seule fois au démarrage**, pas à chaque appel. Renvoyez les lignes utiles, jamais la feuille entière.

`TarifsExcelReader` est fourni : il lit le classeur une seule fois au démarrage. Vous exposez les deux outils à partir de ses méthodes.

### Étape 3 — Base externe

Configurez une seconde source de données, en lecture seule, avec son `@Qualifier`. Exposez `chercherMedicament(nom, espece)` : principe actif, contre-indications pour l'espèce. Les requêtes sont paramétrées dans votre code. N'exposez **jamais** un outil qui exécute du SQL écrit par le modèle.

La seconde source de données (`CatalogueConfig`) est déjà configurée : vous écrivez l'outil, avec des requêtes paramétrées.

### Étape 4 — Google Agenda

L'agenda est caché derrière une interface `AgendaPort` qui a deux implémentations : `GoogleAgendaAdapter` (réel, fourni) et `AgendaSimule` (en mémoire). Les tests utilisent la seconde, parce qu'elle ne dépend ni du réseau ni d'un compte. Vous écrivez `AgendaSimule` et les deux outils ; l'essai avec l'agenda réel est une extension (voir « Extensions »).

`AgendaSimule` démarre avec deux créneaux déjà occupés, le 3 décembre 2026 à 10 h et à 14 h 30 : utilisez ce jour pour vos tests.

```java
public interface AgendaPort {
    List<LocalTime> creneauxLibres(LocalDate jour);                              // lecture : heures de début libres
    String creerEvenement(String titre, LocalDateTime debut, int dureeMinutes);  // écriture : jamais appelée par un outil
}
// choix de l'implémentation par la propriété agenda.mode : simule (défaut) ou google (classe AgendaConfig)
```

| Outil | Comportement |
| --- | --- |
| `creneauxLibres(jour)` | Renvoie les créneaux de 30 minutes libres entre 9 h et 18 h, à partir des événements de l'agenda. |
| `proposerEvenementAgenda(creatureId, dateHeure, motif)` | N'écrit rien. Enregistre une proposition, comme au TD2. |

La confirmation réutilise le mécanisme du TD2. Elle crée un événement intitulé « Consultation — Flammèche (Dragon) » **sans invité** : aucun e-mail ne doit partir.

Question de conception : une consultation existe maintenant dans la base et dans l'agenda. Laquelle fait foi ? Que se passe-t-il quand elles divergent ? Écrivez votre choix dans le journal.

### Étape 5 — Expérience sur le contexte

Le corpus `grand-bestiaire.txt` est trop gros pour être lu en entier avec confort. Chacune des dix questions de `questions-reference.csv` a sa réponse dans un court passage caché quelque part. Comparez deux stratégies sur les dix questions (une troisième est facultative) :

| Stratégie | Principe |
| --- | --- |
| A. Tout dans le prompt | Le corpus entier est placé dans le message système. |
| B. Outil de recherche | `chercherDansBestiaire(motsCles)` renvoie au plus 3 passages de 800 caractères. |
| C. Similarité (facultatif) | Un magasin vectoriel en mémoire renvoie les 3 passages les plus proches. |

Pour chaque question et chaque stratégie, relevez dans `mesures.csv` : les tokens d'entrée et de sortie (lus dans les métadonnées de la réponse : `reponse.getMetadata().getUsage()`), la latence en millisecondes, le coût estimé avec le barème fictif fourni, et l'exactitude (1 si la réponse contient l'élément attendu, sinon 0). Le squelette `ExperienceContexte` lit le CSV de questions et écrit les mesures ; vous codez la fonction `mesurer`.

```csv
question,strategie,tokens_entree,tokens_sortie,latence_ms,cout,exact
Q1,A,48210,35,6120,0.0482,1
Q1,B,1840,42,1310,0.0019,1
```

Extension : faites varier deux paramètres avec la stratégie A : la position du passage utile (début, milieu, fin) et la quantité de texte parasite ajoutée (aucune, moitié, double).

### Étape 6 — Synthèse

Remplissez le tableau de synthèse dans `JOURNAL.md` (une ligne par stratégie : tokens moyens, latence moyenne, coût, exactitude) et écrivez cinq lignes de conclusion, chacune avec un chiffre.

## En cas de blocage

| Symptôme | Cause fréquente | Que faire |
| --- | --- | --- |
| Wikipedia répond 403 | pas de `User-Agent` | ajouter un en-tête qui nomme votre application et un contact |
| Erreur d'authentification Google | agenda non partagé avec le compte de service | vérifier l'identifiant de l'agenda et le partage ; sinon, revenir à `agenda.mode=simule` |
| Tokens à 0 dans les métadonnées | le fournisseur ne les annonce pas | estimer (1 token pour 4 caractères) et le noter dans le journal |
| « context length exceeded » en stratégie A | corpus plus grand que la fenêtre du modèle | c'est un résultat : notez la taille qui fait échouer |
| Le modèle n'appelle pas l'outil | description d'outil trop vague | reformuler la description et les `@ToolParam` |

## Questions de réflexion

1. À partir de quelle taille de corpus la stratégie A échoue-t-elle, et dans quel ordre : erreur, coût, latence, exactitude ?
2. La position du passage utile change-t-elle l'exactitude ? Qu'en déduisez-vous pour la façon d'organiser un prompt ?
3. Que doit renvoyer un outil : tout, ou le strict nécessaire ? Trouvez un cas où en renvoyer moins fait perdre une information utile.
4. Parmi vos quatre outils, lesquels renvoient du texte écrit par un tiers ? Que pourrait contenir ce texte ? Gardez la réponse : le TD6 y reviendra.
5. Votre assistant sait maintenant écrire dans l'agenda. Quelles protections avez-vous mises, et lesquelles manquent encore ?

## Livrable

- Le dossier `td3-outils-externes` : vos outils fonctionnels (Wikipedia, Excel, médicaments, agenda) derrière leurs interfaces, un test unitaire par outil (réponse Wikipedia enregistrée, agenda simulé).
- `mesures.csv` et, dans `JOURNAL.md`, le tableau de synthèse de l'expérience avec cinq lignes de conclusion.

## Extensions

Si le socle est terminé avant la fin de la séance :

- essayez `GoogleAgendaAdapter` : dans Google Agenda, créez un agenda vide dédié au TD, partagez-le avec l'adresse du compte de service (dans le `README` du dossier) avec le droit « Apporter des modifications aux événements », puis renseignez `agenda.mode=google`, `agenda.google.calendar-id` (identifiant de votre agenda) et `agenda.google.credentials-file` (chemin du fichier du compte de service) dans `application-local.yml` ;
- faites varier la position du passage utile (début, milieu, fin) et la quantité de texte parasite (aucune, moitié, double) avec la stratégie A, et notez les résultats dans le journal ;
- implémentez la stratégie C (similarité) et comparez-la à B ;
- écrivez vous-même `TarifsExcelReader` avec Apache POI avant de lire celui qui est fourni.

## Pour aller plus loin

- Mettez en cache les pages Wikipedia déjà lues.
- Faites la stratégie C avec un vrai modèle d'embeddings et comparez-la à B.
- Ajoutez un outil `comparerTarifs(espece1, espece2)` et observez si le modèle l'utilise à bon escient.
