---
title: Construire un duo acteur / critique
resume: Deux agents, un verdict structuré, une boucle bornée avec objections.
td: td5
notions: agents-roles, sortie-structuree, confirmation-ecritures, moindre-privilege
---
## Objectif
Faire vérifier le diagnostic d'un agent par un second agent doté d'**autres sources**.

{{fig:acteur_critique}}

## Les étapes
1. **L'acteur** (vétérinaire) : prompt selon le profil (prudent / pressé), outils de lecture ; il rend un `Diagnostic` structuré.
2. **Le critique** (gardien) : **un seul outil**, `chercherDansWikipedia` ; il rend un `Critique` (`verdict` = `APPROUVE` / `REJETE` / `DOUTE`, `objections`, `sources`).
3. **Boucle** : sur `REJETE`, l'acteur est relancé avec les **objections** ; au plus `max` tours.
4. **Humain** : l'issue (`APPROUVE`, `DOUTE`, ou `REJETE` après `max`) est présentée pour validation ; rien ne s'applique avant.
5. Décrivez le graphe dans un scénario `.md` (voir la recette *Écrire un scénario*).

## Vérifier
- un cas piégé (le bon traitement est contre-indiqué) provoque au moins un `REJETE` ;
- le critique **cite** ses sources ;
- le nombre de tours ne dépasse jamais `max`.

## Pièges
- Critique sans source indépendante : il approuve tout.
- Pas de `max` : la boucle peut ne pas finir.
