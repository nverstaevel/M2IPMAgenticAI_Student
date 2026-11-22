# TD1 — La clinique des créatures fantastiques

## Objectifs de la séance

Construire en 3 h l'application de base, sans IA : API REST, pages Thymeleaf, sécurité et tests. Son architecture en couches est imposée, parce que les TD suivants brancheront un modèle de langage sur ses services : ce que vous écrivez ici sera appelé par un modèle, qui lira vos messages d'erreur.

Ce TD est surtout un TD de **conception**, pas de frappe. Vous modélisez les relations, vous écrivez la règle de gestion qui compte (le chevauchement de créneaux), et vous **prédisez** le comportement de l'API et de la sécurité avant de le vérifier. Le reste (DTO, contrôleurs, pages, sécurité, dépôts, tests) est fourni, lisible et testé : vous le lisez, vous l'essayez, vous l'expliquez.

À la fin de la séance, vous saurez :

1. **structurer** une application Spring Boot en couches (`domain`, `repository`, `service`, `controller`, `dto`) et dire ce que chaque couche a le droit de savoir ;
2. **modéliser** des relations JPA (un propriétaire, plusieurs créatures, plusieurs consultations) et charger un jeu de données ;
3. **exposer une API REST** avec des DTO, une validation d'entrée et des erreurs HTTP (404, 400, 409) lisibles ;
4. **sécuriser** par rôles (PERSONNEL pour écrire, LECTURE pour lire) ;
5. **tester** un service et un endpoint, et exprimer une règle de gestion (le chevauchement de créneaux) à l'endroit où elle est sûre ;
6. **écrire des messages d'erreur** compréhensibles par un humain et par un modèle.

**Vous avez réussi si** `mvn verify` est vert, si `curl -u visiteur:visiteur localhost:8080/api/proprietaires?nom=brume` renvoie Morgane Brumevent, et si la planification de deux consultations qui se chevauchent chez le même soigneur est refusée avec une phrase française claire.

| 🧰 Fourni, prêt à l'emploi | 🧠 À vous |
| --- | --- |
| `pom.xml`, configuration, `data.sql`, DTO, exceptions, **dépôts Spring Data**, contrôleurs REST et web, **gabarits Thymeleaf**, **configuration de sécurité**, **tests** (ils décrivent le comportement attendu), `Espece` et `Creature` (modèle de relation) | les **relations JPA** de `Proprietaire`, `Soigneur` et `Consultation` (3 `TODO`), la **règle de chevauchement** et le diagnostic dans `CliniqueService` (2 `TODO`), les **prédictions** de l'API et de la sécurité, un **test** de plus, le journal |

Chaque étape suit la même forme : **Objectif**, **Pas à pas**, **Point de contrôle**. Ne passez pas à l'étape suivante tant que le point de contrôle n'est pas vert.

## Avant la séance

- [ ] Java 21, Maven et un IDE installés (Git est recommandé, pas obligatoire). `java -version` et `mvn -version` répondent.
- [ ] Le squelette (inspiré de Spring Petclinic) compile : `mvn -q compile` passe avant que vous écriviez quoi que ce soit. L'application ne démarre pas encore : certaines relations ne sont pas mappées (c'est normal, voir l'étape 1). Les tests échouent aussi tant que vous n'avez pas écrit vos cinq `TODO TD1`.
- [ ] À relire : entités JPA et `@OneToMany`, Spring Data, `@RestController`, Thymeleaf, Spring Security. Le support de CM1 contient un rappel Spring Boot avec cette même clinique (*Rappel Spring Boot*).

## Ressources

| Ressource | Pour quoi faire | Quand |
| --- | --- | --- |
| Support de CM1, section « Rappel Spring Boot » | les couches, l'injection, une entité, un service, un test | avant la séance |
| [Spring Boot : documentation de référence](https://docs.spring.io/spring-boot/index.html) | configuration, tests, `data.sql` | toute la séance |
| [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/) | requêtes dérivées, `@Lock` | étapes 2 et 3 |
| [Spring Petclinic](https://github.com/spring-projects/spring-petclinic) | un exemple d'organisation en couches | à consulter, pas à copier |
| [Thymeleaf](https://www.thymeleaf.org/documentation.html) | gabarits, `th:each`, formulaires | étape 5 |
| [Spring Security](https://docs.spring.io/spring-security/reference/) | chaîne de filtres, utilisateurs en mémoire | étape 6 |
| [Hibernate Validator](https://hibernate.org/validator/) | contraintes `@NotBlank`, `@NotNull` | étape 4 |

## Le modèle de données

| Entité | Champs | Relations |
| --- | --- | --- |
| Proprietaire | id, nom, prenom, ville, telephone | possède plusieurs créatures |
| Espece | id, nom, habitat | une espèce par créature |
| Creature | id, nom, dateNaissance, espece, proprietaire | a plusieurs consultations |
| Soigneur | id, nom, especesSoignees | soigne plusieurs espèces |
| Consultation | id, dateHeure, creature, soigneur, motif, diagnostic, traitement, tarif | diagnostic et traitement vides tant que la consultation n'a pas eu lieu |

Le jeu de données du squelette est loufoque à dessein : un dragon enrhumé, une licorne au sabot fêlé, un yéti allergique à l'été, un loup-garou insomniaque, un vampire anémique, un phénix qui récidive. Un extrait figure en annexe.

## Parcours guidé

| Étape | Durée | Vous faites | Point de contrôle |
| --- | --- | --- | --- |
| 1. Lire le modèle | 25 min | lire `data.sql`, remplir le tableau de conception | tableau de relations rempli, erreur de démarrage expliquée |
| 2. Écrire les relations | 25 min | 3 `TODO TD1` dans `domain` | l'application démarre ; un propriétaire retrouve ses créatures |
| 3. La règle de gestion | 40 min | 2 `TODO TD1` dans `CliniqueService` | `CliniqueServiceTest` est vert |
| 4. Lire l'API | 30 min | prédire puis vérifier les codes HTTP | tableau de prédictions : au moins 5 sur 6 justes |
| 5. Lire les pages | 20 min | relier chaque page à la méthode de service qu'elle appelle | une consultation planifiée depuis le navigateur |
| 6. Prédire la sécurité | 20 min | matrice rôle × requête | `ApiTest` est vert |
| 7. Un test de plus | 20 min | un test de votre choix | `mvn verify` est vert |

### Étape 1 — Lire le modèle

**Objectif.** Comprendre ce que `data.sql` attend, avant d'écrire la moindre relation.

**Pas à pas.**

1. 🧰 Ouvrez le squelette, lisez `src/main/resources/data.sql` et `application.properties`. Les tables ne sont pas écrites à la main : Hibernate les crée à partir des entités (`ddl-auto=create-drop`), puis exécute `data.sql`.
2. Lancez l'application (`CliniqueApplication`, ou `mvn spring-boot:run`). Elle échoue : notez dans votre journal la **première** ligne de l'erreur. C'est le résultat attendu, car trois relations ne sont pas encore mappées.
3. 🧠 Remplissez ce tableau de conception dans `JOURNAL.md`. La dernière colonne est la plus importante : qui porte la clé étrangère ?

| Relation | Cardinalité (1-N, N-N…) | Obligatoire ? | Colonne ou table de jointure dans `data.sql` | Quelle entité porte la clé ? |
| --- | --- | --- | --- | --- |
| Créature → Espèce | | | `espece_id` | |
| Créature → Propriétaire | | | | |
| Consultation → Créature | | | | |
| Consultation → Soigneur | | | | |
| Soigneur ↔ Espèce | | | `soigneur_espece` | |

**Point de contrôle.** Le tableau est rempli sans case vide, et vous pouvez dire à votre voisin pourquoi on n'écrit pas de colonne `creature_id` dans `Proprietaire`.

### Étape 2 — Écrire les relations

**Objectif.** Traduire le tableau en annotations JPA. Notion : *Entités JPA*. Recette : *Entités et données*.

**Pas à pas.**

1. 🧰 `Creature` est écrite en entier : c'est votre modèle. Repérez `@ManyToOne(fetch = FetchType.LAZY, optional = false)` : « plusieurs créatures pour une espèce, l'espèce est obligatoire, on ne la charge que si on la lit ».
2. 🧠 Dans `Consultation`, complétez les deux relations vers `Creature` et `Soigneur` sur le modèle de `Creature.espece`.
3. 🧠 Dans `Proprietaire`, la liste `creatures` est le **côté inverse** : une annotation `@OneToMany` avec `mappedBy`, sans clé étrangère de ce côté. Cherchez dans `Creature` le nom du champ à citer.
4. 🧠 Dans `Soigneur`, la relation plusieurs-à-plusieurs passe par la table `soigneur_espece` :

```java
@ManyToMany
@JoinTable(name = "soigneur_espece",
        joinColumns = @JoinColumn(name = "soigneur_id"),
        inverseJoinColumns = @JoinColumn(name = "espece_id"))
private Set<Espece> especesSoignees = new HashSet<>();
```

5. Relancez l'application.

**Point de contrôle.** L'application démarre sans erreur. Dans les tests, `unProprietaireRetrouveSesCreatures` n'est pas encore vert (le service n'est pas fini), mais `data.sql` s'exécute : « Brumevent » possède Paillette et Rebelote.

### Étape 3 — La règle de gestion

**Objectif.** Écrire la règle qui rend `CliniqueService` utile au modèle de langage.

Les dépôts et les cinq méthodes de lecture sont fournis. Les signatures des sept méthodes sont **imposées** : les outils du TD2 les appelleront telles quelles.

| Méthode | Rôle | Lecture ou écriture | Écrite ? |
| --- | --- | --- | --- |
| `rechercherProprietaires(nom)` | recherche partielle, insensible à la casse | lecture | 🧰 fournie |
| `trouverProprietaire(id)` | une fiche | lecture | 🧰 fournie |
| `creaturesDuProprietaire(proprietaireId)` | créatures d'un propriétaire | lecture | 🧰 fournie |
| `consultationsDeLaCreature(creatureId)` | historique, plus récent d'abord | lecture | 🧰 fournie |
| `soigneursPourEspece(espece)` | soigneurs compétents | lecture | 🧰 fournie |
| `planifierConsultation(creatureId, soigneurId, dateHeure, motif)` | crée une consultation | écriture | 🧠 à vous |
| `enregistrerDiagnostic(consultationId, diagnostic, traitement)` | clôt une consultation | écriture | 🧠 à vous |

Deux règles de gestion : une consultation dure 30 minutes ; un soigneur ne peut pas avoir deux consultations qui se chevauchent (`ConflitDeCreneauException`). Un diagnostic ne s'écrit qu'une fois.

**Pas à pas.**

1. 🧰 Lisez `rechercherProprietaires` et `creaturesDuProprietaire` : le motif est toujours le même (vérifier que la ressource existe, appeler le dépôt, convertir en DTO).
2. 🧰 Lisez `ConsultationRepository.existsBySoigneurIdAndDateHeureGreaterThanAndDateHeureLessThan` : elle répond « le soigneur a-t-il une consultation qui **commence** strictement entre ces deux bornes ? ». Lisez aussi `SoigneurRepository.findByIdPourPlanification` : le **verrou** sert à sérialiser deux demandes simultanées.
3. 🧠 Sur papier, avec l'exemple 14 h 00 déjà pris, cherchez les bornes pour refuser 14 h 10, 13 h 40, 13 h 30 (accepté ?) et 14 h 30 (accepté ?). Deux intervalles `[a1, a2[` et `[b1, b2[` se chevauchent si `a1 < b2 && b1 < a2`.
4. 🧠 Écrivez `planifierConsultation` en suivant les cinq temps du commentaire. Le message du conflit doit être compris par un humain **et** par un modèle : « Le soigneur 3 (Dr Gribouille) est déjà pris le 12/03 à 14h00 : une consultation dure 30 minutes. »
5. 🧠 Écrivez `enregistrerDiagnostic` (trois lignes, voir le commentaire).
6. Lancez le test du service :

```bash
mvn test -Dtest=CliniqueServiceTest
```

**Point de contrôle.** `CliniqueServiceTest` est vert. Question de conception, à écrire dans le journal : pourquoi cette règle est-elle dans le **service** et pas dans le contrôleur ? Que se passerait-il sans le verrou si deux requêtes arrivaient en même temps ?

### Étape 4 — Lire l'API

**Objectif.** Savoir dire, avant de lancer une requête, quel code HTTP et quel message vous obtiendrez. Notion : *API REST et validation*. Recette : *Endpoint REST*.

**Pas à pas.**

1. 🧰 Lisez `ApiController` (les endpoints), les DTO (des `record` : jamais d'entités) et `ApiExceptionHandler` (qui traduit chaque exception en code HTTP). `PlanificationRequest` porte la validation.
2. 🧠 **Avant d'essayer**, recopiez le tableau dans votre journal et prédisez la colonne « Code attendu ».

| Requête (`-u gribouille:gribouille`) | Code attendu | Observé |
| --- | --- | --- |
| `GET /api/proprietaires?nom=brume` | | |
| `GET /api/creatures/9999` | | |
| `POST /api/consultations` avec `{}` | | |
| `POST /api/consultations` sur un créneau libre | | |
| le même `POST`, une deuxième fois | | |
| `POST /api/consultations` avec `"dateHeure":"demain"` | | |

3. Vérifiez en lançant l'application :

```bash
curl -u gribouille:gribouille 'localhost:8080/api/proprietaires?nom=brume'
curl -u gribouille:gribouille localhost:8080/api/creatures/9999
curl -u gribouille:gribouille -H 'Content-Type: application/json' \
  -d '{"creatureId":1,"soigneurId":1,"dateHeure":"2027-03-12T14:00:00","motif":"toux"}' localhost:8080/api/consultations
```

**Point de contrôle.** Au moins 5 prédictions justes sur 6. Pour chaque erreur de prédiction, une ligne dans le journal : qu'aviez-vous mal compris ?

### Étape 5 — Lire les pages

**Objectif.** Relier ce que l'utilisateur voit à la couche service.

**Pas à pas.**

1. 🧰 Lancez l'application et ouvrez <http://localhost:8080> (connexion `gribouille` / `gribouille`). Cherchez « brume », ouvrez la fiche, planifiez une consultation.
2. 🧠 Ouvrez `WebController` : pour chacune des trois pages, notez dans le journal **quelle méthode de `CliniqueService`** elle appelle. Elles n'appellent pas l'API REST.
3. 🧠 Planifiez deux fois la même consultation : quel message voit l'utilisateur ? D'où vient-il ?

**Point de contrôle.** Une consultation planifiée depuis le navigateur apparaît sur la fiche du propriétaire ; la seconde demande affiche le message du conflit.

### Étape 6 — Prédire la sécurité

**Objectif.** Savoir qui a le droit de faire quoi. Notion : *Sécurité basique*. Recette : *Sécuriser l'API*.

**Pas à pas.**

1. 🧰 Lisez `SecurityConfig` : deux utilisateurs en mémoire (`gribouille`, rôle PERSONNEL ; `visiteur`, rôle LECTURE), mot de passe = identifiant en clair (`{noop}`, **jamais ailleurs que dans un TD**), CSRF ignoré pour `/api/**` seulement.
2. 🧠 Remplissez la matrice **avant** de tester (pour l'anonyme, deux réponses sont possibles : redirection vers la page de connexion, ou 401), puis vérifiez avec `curl` (sans `-u` pour l'anonyme, `-u visiteur:visiteur` pour le visiteur) :

| Requête | anonyme | `visiteur` | `gribouille` |
| --- | --- | --- | --- |
| `GET /api/proprietaires` | | | |
| `POST /api/consultations` | | | |

3. Lancez `ApiTest` : il vérifie ces cas.

```bash
mvn test -Dtest=ApiTest
```

**Point de contrôle.** `ApiTest` est vert et votre matrice est juste. Question de réflexion : pourquoi le CSRF est-il ignoré pour l'API mais pas pour les pages ?

### Étape 7 — Un test de plus

**Objectif.** Exprimer une règle de gestion sous forme de test. Recette : *Règle métier et test*.

**Pas à pas.**

1. 🧰 Lisez dans `CliniqueServiceTest` le test de chevauchement : il suit « préparer, agir, vérifier l'exception **et son message** ».
2. 🧠 Écrivez **un** test de plus, au choix : un second diagnostic est refusé ; deux soigneurs différents peuvent prendre le même créneau ; une consultation à exactement 30 minutes d'une autre est acceptée.

```java
@Test
void deuxConsultationsQuiSeChevauchentSontRefusees() {
    service.planifierConsultation(1L, 1L, LocalDateTime.of(2027, 3, 12, 14, 0), "toux");
    assertThatThrownBy(() -> service.planifierConsultation(2L, 1L, LocalDateTime.of(2027, 3, 12, 14, 10), "sabot"))
        .isInstanceOf(ConflitDeCreneauException.class)
        .hasMessageContaining("déjà pris");
}
```

3. Lancez tout et complétez `JOURNAL.md` :

```bash
mvn verify
```

**Point de contrôle.** `mvn verify` est vert, avec votre test en plus.

## En cas de blocage

| Symptôme | Cause fréquente | Que faire |
| --- | --- | --- |
| Erreur au démarrage sur une association (`Consultation.creature`, `Soigneur.especesSoignees`…) | relation pas encore mappée | c'est l'étape 2 : relire le tableau de l'étape 1 |
| `Table "SOIGNEUR_ESPECE" not found` ou une colonne introuvable | nom de table ou de colonne qui ne correspond pas à `data.sql` | comparer l'annotation `@JoinTable` au tableau de l'étape 1 |
| `UnsupportedOperationException : À écrire` | un `TODO TD1` n'est pas encore écrit | c'est normal tant que vous n'avez pas fait l'étape : repérez le `TODO TD1` correspondant |
| `data.sql` s'exécute avant la création des tables | initialisation différée absente | `spring.jpa.defer-datasource-initialization=true` |
| Clés étrangères qui ne correspondent pas | identifiants générés écrits en dur | retrouver par le nom : `(SELECT id FROM espece WHERE nom = 'Dragon')` (voir l'annexe) |
| `LazyInitializationException` | entité exposée hors de la transaction | renvoyer des DTO, construits dans le service |
| 403 sur un POST | CSRF | ignorer CSRF pour `/api/**` |
| Pas de console H2 | retirée de Spring Boot 4 | inspecter les données par l'API |

## Questions de réflexion

1. Pourquoi exposer des DTO plutôt que des entités JPA ? Qu'est-ce qui fuite si vous exposez l'entité `Proprietaire` telle quelle ?
2. Où mettre la règle de chevauchement : contrôleur, service, contrainte en base ? Que se passe-t-il si deux requêtes arrivent en même temps ?
3. Quelles méthodes sont des lectures, lesquelles des écritures ? Cette distinction vous servira dès le TD2.

## Livrable

- Le dossier `td1-clinique` : l'application démarre, les pages et l'API fonctionnent, `mvn verify` est vert, avec votre test en plus.
- Dans `JOURNAL.md` : le tableau de conception (étape 1), les prédictions de l'API (étape 4) et de la sécurité (étape 6), les réponses aux deux questions de conception.

## Pour aller plus loin

- Pagination de la liste des propriétaires.
- Une méthode `statistiquesParEspece()` : nombre de consultations et tarif moyen par espèce.
- Une page d'accueil qui affiche la créature du jour.

## Annexe : extrait du jeu de données

Cet extrait montre les données avec des identifiants écrits en dur, pour que vous voyiez les liens. Le vrai `data.sql` fourni n'écrit aucun identifiant : il retrouve les clés étrangères par le nom, par exemple `(SELECT id FROM espece WHERE nom = 'Dragon')`. C'est plus robuste avec des identifiants générés.

```sql
INSERT INTO espece (id, nom, habitat) VALUES
  (1, 'Dragon', 'montagne'), (2, 'Licorne', 'forêt'), (3, 'Yéti', 'glacier'),
  (4, 'Loup-garou', 'forêt'), (5, 'Vampire', 'château'), (6, 'Phénix', 'volcan');

INSERT INTO proprietaire (id, nom, prenom, ville, telephone) VALUES
  (1, 'Brumevent', 'Morgane', 'Brécilien', '0600000001'),
  (2, 'Fumerolle', 'Gaspard', 'Pic-du-Dragon', '0600000002'),
  (3, 'Clairdelune', 'Ondine', 'Val-des-Ombres', '0600000003');

INSERT INTO creature (id, nom, date_naissance, espece_id, proprietaire_id) VALUES
  (1, 'Flammèche', '1824-03-02', 1, 2),
  (2, 'Paillette', '1990-06-21', 2, 1),
  (3, 'Grognon', '1957-12-01', 3, 2),
  (4, 'Lunatique', '1988-10-31', 4, 3),
  (5, 'Monsieur Crépuscule', '1731-02-14', 5, 3),
  (6, 'Rebelote', '1203-04-01', 6, 1);
```
