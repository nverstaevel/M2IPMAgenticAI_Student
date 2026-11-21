# TD1 — La clinique des créatures fantastiques

## Objectifs de la séance

Construire en 3 h l'application de base, sans IA : API REST, pages Thymeleaf, sécurité et tests. Son architecture en couches est imposée, parce que les TD suivants brancheront un modèle de langage sur ses services : ce que vous écrivez ici sera appelé par un modèle, qui lira vos messages d'erreur.

À la fin de la séance, vous saurez :

1. **structurer** une application Spring Boot en couches (`domain`, `repository`, `service`, `controller`, `dto`) et dire ce que chaque couche a le droit de savoir ;
2. **modéliser** des relations JPA (un propriétaire, plusieurs créatures, plusieurs consultations) et charger un jeu de données ;
3. **exposer une API REST** avec des DTO, une validation d'entrée et des erreurs HTTP (404, 400, 409) lisibles ;
4. **sécuriser** par rôles (PERSONNEL pour écrire, LECTURE pour lire) ;
5. **tester** un service et un endpoint, et exprimer une règle de gestion (le chevauchement de créneaux) à l'endroit où elle est sûre ;
6. **écrire des messages d'erreur** compréhensibles par un humain et par un modèle.

**Vous avez réussi si** `mvn verify` est vert, si `curl -u visiteur:visiteur localhost:8080/api/proprietaires?nom=brume` renvoie Morgane Brumevent, et si la planification de deux consultations qui se chevauchent chez le même soigneur est refusée avec une phrase française claire.

| Vous écrivez | Fourni dans le squelette |
| --- | --- |
| entités, repositories, `CliniqueService`, contrôleurs, DTO, gabarits de pages, configuration de sécurité, tests | `pom.xml` (versions fixées), configuration, `data.sql`, gabarits Thymeleaf de base, fichier `JOURNAL.md` vide |

## Avant la séance

- [ ] Java 21, Maven et un IDE installés (Git est recommandé, pas obligatoire). `java -version` et `mvn -version` répondent.
- [ ] Le squelette (inspiré de Spring Petclinic) compile : `mvn -q compile` passe avant que vous écriviez quoi que ce soit. L'application ne démarre pas encore : il n'y a aucune entité, donc aucune table pour `data.sql` (c'est normal, voir l'étape 1).
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
| 1. Mise en route | 15 min | ouvrir, compiler, lire `data.sql`, voir l'échec attendu au démarrage | vous savez dire quelles tables et colonnes `data.sql` attend |
| 2. Entités et données | 35 min | cinq entités, relations, `data.sql` | un propriétaire retrouve ses créatures |
| 3. Repositories et services | 30 min | Spring Data, `CliniqueService` | les sept signatures imposées existent et compilent |
| 4. API REST | 35 min | DTO, validation, `@ControllerAdvice` | 200, 404, 400 et 409 sur les bons cas |
| 5. Pages Thymeleaf | 30 min | recherche, fiche, formulaire | une consultation planifiée depuis le navigateur |
| 6. Sécurité | 15 min | deux utilisateurs, rôles | `visiteur` reçoit 403 en écriture |
| 7. Tests | 20 min | un test de service, un test `MockMvc` | `mvn verify` est vert |

### Étape 1 — Mise en route

Ouvrez le squelette et lisez `src/main/resources/data.sql` et `application.properties`. Les tables ne sont pas écrites à la main : Hibernate les crée à partir de **vos entités** (`ddl-auto=create-drop`), puis exécute `data.sql`. Lancez quand même l'application (`CliniqueApplication`, ou `mvn spring-boot:run`) : elle échoue avec une erreur du type *Table "ESPECE" not found*. C'est le résultat attendu : sans entités, il n'y a rien à remplir. Retenez cette erreur, vous la reverrez si une entité ne correspond pas à `data.sql`.

Point de contrôle : vous pouvez remplir ce tableau à partir de `data.sql` (il vous sert à l'étape 2).

| Table | Colonnes lues dans `data.sql` |
| --- | --- |
| `espece` | `nom`, `habitat` |
| `proprietaire` | `nom`, `prenom`, `ville`, `telephone` |
| `creature` | `nom`, `date_naissance`, `espece_id`, `proprietaire_id` |
| `soigneur` | `nom` |
| `soigneur_espece` | `soigneur_id`, `espece_id` (table de jointure) |
| `consultation` | `date_heure`, `creature_id`, `soigneur_id`, `motif`, `diagnostic`, `traitement`, `tarif` |

### Étape 2 — Entités et données

Écrivez les cinq entités et leurs relations **pour que les noms collent à `data.sql`** (tableau de l'étape 1). Spring transforme `dateNaissance` en `date_naissance` et `@ManyToOne Espece espece` en colonne `espece_id`. La relation plusieurs-à-plusieurs entre soigneurs et espèces doit utiliser la table `soigneur_espece`. Un exemple de relation et de requête dérivée :

```java
@Entity
public class Creature {
    @Id @GeneratedValue private Long id;
    private String nom;
    private LocalDate dateNaissance;
    @ManyToOne(optional = false) private Espece espece;
    @ManyToOne(optional = false) private Proprietaire proprietaire;
    // constructeur sans argument pour JPA, accesseurs
}

public interface ProprietaireRepository extends JpaRepository<Proprietaire, Long> {
    List<Proprietaire> findByNomContainingIgnoreCase(String nom);   // recherche partielle, insensible à la casse
}
```

La table de jointure se déclare ainsi (à placer sur `Soigneur`) :

```java
@ManyToMany
@JoinTable(name = "soigneur_espece",
        joinColumns = @JoinColumn(name = "soigneur_id"),
        inverseJoinColumns = @JoinColumn(name = "espece_id"))
private Set<Espece> especesSoignees = new HashSet<>();
```

Point de contrôle : l'application démarre sans erreur et un propriétaire retrouve ses créatures : « Brumevent » possède Paillette et Rebelote.

### Étape 3 — Repositories et services

Créez les repositories Spring Data et la couche service. Les signatures ci-dessous sont **imposées** : les outils du TD2 les appelleront telles quelles.

| Méthode | Rôle | Lecture ou écriture |
| --- | --- | --- |
| `List<ProprietaireDto> rechercherProprietaires(String nom)` | recherche partielle, insensible à la casse | lecture |
| `ProprietaireDto trouverProprietaire(Long id)` | une fiche | lecture |
| `List<CreatureDto> creaturesDuProprietaire(Long proprietaireId)` | créatures d'un propriétaire | lecture |
| `List<ConsultationDto> consultationsDeLaCreature(Long creatureId)` | historique, plus récent d'abord | lecture |
| `List<SoigneurDto> soigneursPourEspece(String espece)` | soigneurs compétents | lecture |
| `ConsultationDto planifierConsultation(Long creatureId, Long soigneurId, LocalDateTime dateHeure, String motif)` | crée une consultation | écriture |
| `void enregistrerDiagnostic(Long consultationId, String diagnostic, String traitement)` | clôt une consultation | écriture |

Deux règles de gestion sont obligatoires. Une consultation dure 30 minutes. Un soigneur ne peut pas avoir deux consultations qui se chevauchent : `planifierConsultation` lève alors une `ConflitDeCreneauException`.

Pour détecter un chevauchement, rappelez-vous que deux intervalles `[début1, fin1[` et `[début2, fin2[` se chevauchent si `début1 < fin2 && début2 < fin1`. Comparez la nouvelle consultation (début, début + 30 minutes) aux consultations du même soigneur. Annotez la méthode d'écriture avec `@Transactional`.

Tous les messages d'erreur doivent être lisibles par un humain **et** par un modèle, par exemple « Aucune créature d'identifiant 42 » ou « Le soigneur 3 est déjà pris le 12/03 à 14 h ». Ils seront relus par l'assistant au TD2.

### Étape 4 — API REST

Exposez ces services en JSON avec des DTO (des `record` Java), jamais les entités. Validez les entrées avec Bean Validation. Traduisez les erreurs en 404, 400 et 409 avec un `@ControllerAdvice`.

```java
@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ConflitDeCreneauException.class)
    ProblemDetail conflit(ConflitDeCreneauException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());   // detail = phrase lisible
        p.setTitle("Conflit de créneau");
        return p;
    }
}
```

Premiers essais :

```bash
curl -u visiteur:visiteur 'localhost:8080/api/proprietaires?nom=brume'     # 200, Morgane Brumevent
curl -u visiteur:visiteur localhost:8080/api/creatures/9999                 # 404, "Aucune créature d'identifiant 9999"
```

### Étape 5 — Pages Thymeleaf

Une page de recherche de propriétaires, une fiche propriétaire avec ses créatures et leurs consultations, un formulaire de nouvelle consultation. Les pages appellent la couche service, pas l'API REST.

### Étape 6 — Sécurité

Connexion par formulaire avec deux utilisateurs en mémoire : `gribouille` (rôle PERSONNEL, lecture et écriture) et `visiteur` (rôle LECTURE). Les écritures REST exigent PERSONNEL. Pour le TD, mot de passe = identifiant, en clair (`{noop}`) : jamais ailleurs.

```java
@Bean
SecurityFilterChain chaine(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(a -> a
            .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("PERSONNEL", "LECTURE")
            .requestMatchers("/api/**").hasRole("PERSONNEL")        // POST, PUT, DELETE
            .anyRequest().authenticated())
        .formLogin(Customizer.withDefaults())
        .httpBasic(Customizer.withDefaults());
    return http.build();
}
```

Si vos appels REST d'écriture sont refusés par la protection CSRF, ignorez-la pour `/api/**` (jamais pour les pages).

### Étape 7 — Tests

Un test de service avec la base de test, et un test `MockMvc` d'un endpoint. Ajoutez vos premières lignes à `JOURNAL.md`.

```java
@Test
void deuxConsultationsQuiSeChevauchentSontRefusees() {
    service.planifierConsultation(1L, 1L, LocalDateTime.of(2027, 3, 12, 14, 0), "toux");
    assertThatThrownBy(() -> service.planifierConsultation(2L, 1L, LocalDateTime.of(2027, 3, 12, 14, 10), "sabot"))
        .isInstanceOf(ConflitDeCreneauException.class)
        .hasMessageContaining("déjà pris");
}
```

## En cas de blocage

| Symptôme | Cause fréquente | Que faire |
| --- | --- | --- |
| `Table "ESPECE" not found` (ou une autre table) au démarrage | pas encore d'entité, ou noms qui ne correspondent pas à `data.sql` | comparer vos entités au tableau de l'étape 1 (noms de tables et de colonnes) |
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

- Le dossier `td1-clinique` : l'application démarre, les pages et l'API fonctionnent, les tests passent.
- Les premières entrées de `JOURNAL.md`.

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
