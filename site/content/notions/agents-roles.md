---
title: Agents à rôles : acteur, critique, superviseur
resume: Séparer ceux qui proposent, ceux qui vérifient et ceux qui coordonnent.
cm: CM2 — Un modèle en critique un autre ; Acteur et critique : un tour de boucle ; Un superviseur qui délègue
td: td5, td6
---
## L'idée
Au lieu d'un seul agent qui fait tout, on **sépare les rôles** : ceux qui proposent, ceux qui vérifient, ceux qui coordonnent.

## Comprendre
**Acteur / critique.** L'acteur (vétérinaire) propose ; le critique (gardien du bestiaire) vérifie avec **d'autres sources** et rend un verdict `APPROUVE`, `REJETE` ou `DOUTE`. Un rejet renvoie à l'acteur, avec les objections, dans la limite de `max` tours.

{{fig:acteur_critique}}

**Superviseur / travailleurs.** Un planificateur découpe la tâche, des travailleurs l'exécutent chacun avec leurs outils, une synthèse assemble.

{{fig:superviseur}}

À **mesurer**, pas à supposer : rejets corrects, faux rejets, faux accords, tours moyens, tokens, comparés à un **workflow classique** (le même enchaînement écrit en Java).

## À retenir
- Chaque rôle a un travail étroit, donc un prompt court et des outils peu nombreux.
- Plus de rôles = plus d'appels = plus de tokens : le bénéfice doit se **mesurer**.

## Erreurs fréquentes
- Un critique qui n'a pas d'autre source que l'acteur : il valide ce qu'il a déjà lu.
- Une boucle acteur–critique sans `max` de tours.

## Auto-test
Q: Pourquoi le critique ne doit-il pas avoir d'outil d'écriture ?
R: Son rôle est de vérifier ; un outil d'écriture le rendrait détournable et mélangerait contrôle et exécution.
