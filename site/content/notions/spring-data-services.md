---
title: Dépôt Spring Data et service transactionnel
resume: Une interface suffit pour le dépôt ; le service décide, la transaction protège.
cm: CM1 — Rappel Spring Boot : Un dépôt Spring Data ; Le service porte les règles et les transactions
td: td1
famille: spring
ordre: 3
---
## L'idée
Un **dépôt** Spring Data s'écrit avec une interface seulement ; un **service** transactionnel décide et protège les écritures.

## Comprendre
```java
interface CreatureRepository extends JpaRepository<Creature, Long> {
    List<Creature> findByNomContainingIgnoreCase(String fragment);   // la requête est déduite du nom
}
```
- `findById`, `save`, `findAll` sont fournis.
- Une méthode dont le **nom** suit la convention (`findBy…Containing…IgnoreCase`) génère sa requête.
- `@Transactional` sur une méthode de service : tout réussit, ou rien n'est écrit. `readOnly = true` pour les lectures.

```
service.planifierConsultation(...)   →  vérifie  →  repository.save(...)   →  COMMIT (ou ROLLBACK si exception)
```

## Dans la clinique
Un test de service crée une consultation, tente d'en créer une qui chevauche, et vérifie l'**exception** et son **message** : c'est ce message qui remontera dans le chat au TD2.

## À retenir
- Le dépôt ne contient aucune règle ; le service en contient toutes.
- Une exception métier (`RuntimeException` dédiée) annule la transaction.

## Erreurs fréquentes
- Attraper l'exception dans le service et la « manger » : la transaction se valide quand même.
- Oublier `@Transactional` sur une méthode d'écriture composée de plusieurs `save`.

## Auto-test
<details class="qa" markdown="1"><summary>Que se passe-t-il si le service lève une exception métier au milieu d'une transaction ?</summary>

La transaction est annulée (*rollback*) : aucune écriture partielle n'est conservée.

</details>

Q: Que fait Spring Data de la méthode `findByNomContainingIgnoreCase(String nom)` ?
R: Il en déduit la requête à partir du nom : recherche partielle, insensible à la casse, sans que vous écriviez de SQL.

Q: Pourquoi annoter une méthode d'écriture du service avec `@Transactional` ?
R: Pour que la vérification et l'écriture forment un tout : en cas d'exception, rien n'est enregistré.
