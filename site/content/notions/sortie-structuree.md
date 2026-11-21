---
title: Sortie structurée
resume: Texte libre en entrée, objet Java en sortie : « .entity(Record.class) ».
cm: CM1 — ChatClient en quelques lignes ; CM2 — Un modèle en critique un autre
td: td2, td5
famille: modele
ordre: 6
---
## L'idée
Au lieu de parser une phrase, on demande au modèle de **remplir un objet Java** : `.entity(Triage.class)`.

## Comprendre
{{fig:sortie_structuree}}

Spring AI ajoute au prompt le schéma attendu, puis convertit la réponse en objet. Une `enum` limite les valeurs possibles.

```java
record Triage(String espece, String symptome, Urgence urgence) {}
Triage t = clientTriage.prompt().user(texte).call().entity(Triage.class);
```

## Dans la clinique
- TD2 : triage d'une demande libre en `Triage`.
- TD5 : les agents rendent un `Diagnostic` ou un `Critique` structuré, ce qui permet de **chaîner** leurs réponses.

## À retenir
- Utilisez un client **séparé**, sans mémoire ni outils.
- Prévoyez le cas où le format est mauvais (nouvel essai, message clair).

## Erreurs fréquentes
- `record` trop riche : plus il y a de champs, plus le modèle se trompe.
- Mélanger sortie structurée et outils dans le même appel sur un fournisseur qui les gère mal.

## Auto-test
Q: Pourquoi une `enum` pour l'urgence ?
R: Elle restreint les réponses possibles : `ELEVEE` est valide, « assez urgent » ne l'est pas.
