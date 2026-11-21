---
title: Dépôt Spring Data et service transactionnel
resume: Une interface suffit pour le dépôt ; le service décide, la transaction protège.
cm: CM1 — Rappel Spring Boot : Un dépôt Spring Data ; Le service porte les règles et les transactions
td: td1
---
**Dépôt** : une interface qui étend `JpaRepository<Entite, Long>` fournit `findById`, `save`, etc. Des méthodes dérivées du nom (`findByNomContainingIgnoreCase`) génèrent la requête.

**Service** : annoté `@Service`, il enchaîne les appels de dépôts, applique les règles et lève des **exceptions métier** (`ConflitDeCreneauException`). `@Transactional` garantit que tout réussit ou rien n'est écrit ; `readOnly = true` pour les lectures.

**Test** : un test de service avec `@SpringBootTest` utilise la base de test et vérifie la règle (voir l'exemple de l'étape 7 du TD1).
