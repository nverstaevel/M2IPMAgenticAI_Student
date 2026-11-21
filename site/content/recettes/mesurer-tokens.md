---
title: Mesurer tokens, latence, coût et exactitude
resume: Lire `Usage`, chronométrer, appliquer un barème, remplir `mesures.csv`.
td: td2, td3, td4, td5
famille: outils
ordre: 8
notions: tokens-contexte, evaluer-agent, interroger-plutot-que-charger
---
## Squelette
```java
long debut = System.nanoTime();
ChatResponse reponse = client.prompt().user(question).call().chatResponse();
long latenceMs = (System.nanoTime() - debut) / 1_000_000;

Usage usage = reponse.getMetadata().getUsage();
int entree = usage.getPromptTokens();
int sortie = usage.getCompletionTokens();
double cout = entree * BAREME_ENTREE + sortie * BAREME_SORTIE;      // barème fictif fourni
String texte = reponse.getResult().getOutput().getText();
int exact = texte.toLowerCase().contains(attendu.toLowerCase()) ? 1 : 0;
```

## Ligne du CSV
```csv
question,strategie,tokens_entree,tokens_sortie,latence_ms,cout,exact
Q1,A,48210,35,6120,0.0482,1
```

## Pièges
- Usage à `null` ou à 0 : le fournisseur ne l'annonce pas ; estimez (≈ 4 caractères par token) et dites-le.
- Une seule mesure ne prouve rien (voir *Évaluer*) : relancez, notez la dispersion.
- Comparez des choses comparables : même modèle, même température.
