---
title: Tester sans vrai modèle (profil simule)
resume: Piloter le modèle simulé pour tester outils, boucle et arrêts.
td: td2, td3, td4, td5
notions: tester-sans-modele, arrets-agent, appel-outils
---
## Quand l'utiliser
Dans **tous** vos tests : aucun test n'appelle un vrai modèle.

## Principe
Le modèle simulé du dossier fourni répond de façon déterministe. Une réponse de la forme

```
outil:consulterTarif:{"acte":"Consultation","espece":"Dragon"}
```

lui fait **demander** l'outil `consulterTarif` avec ces arguments. Un autre texte est renvoyé tel quel comme réponse finale.

## Exemples de tests
```java
// un outil se teste directement, sans modèle
assertThat(outils.consulterTarif("Consultation", "Dragon")).contains("80,00");

// un arrêt se teste avec un modèle simulé qui répète le même appel
// -> attendu : motif BOUCLE_DETECTEE
```

## Pièges
- Ne comparez jamais la phrase exacte d'un vrai modèle dans un test.
- Un test qui a besoin du réseau ou d'une clé n'est pas un test unitaire : utilisez le port simulé (`AgendaSimule`, client Wikipédia de test).
