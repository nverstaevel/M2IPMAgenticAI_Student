---
title: Lire un classeur Excel une seule fois
resume: Charger le `.xlsx` au démarrage, interroger une structure en mémoire.
td: td3
notions: appel-outils, interroger-plutot-que-charger, tester-sans-modele
---
Le lecteur de classeur est **fourni** au TD3 (`TarifsExcelReader`) : lisez-le comme modèle.

## Principe
```java
// au démarrage (constructeur) : lecture complète, puis structure en mémoire
Map<String, Double> tarifs = new HashMap<>();
try (Workbook classeur = WorkbookFactory.create(chemin)) {
    for (Row ligne : classeur.getSheet("Tarifs")) {
        // lire les cellules, ignorer l'en-tête
    }
}
// à l'appel de l'outil : simple recherche, aucune lecture de fichier
```

## Ce que doit faire l'outil
- trouver la ligne par **acte + espèce**, sans tenir compte de la casse ;
- renvoyer **une phrase** avec le prix formaté (`80,00 €`), pas la feuille ;
- répondre « Aucun tarif … » plutôt que de lever une exception si l'acte est inconnu.

## Pièges
- Lire le fichier à chaque appel : lent et inutile.
- `Cell.getNumericCellValue()` sur une cellule texte : exception ; testez le type.
