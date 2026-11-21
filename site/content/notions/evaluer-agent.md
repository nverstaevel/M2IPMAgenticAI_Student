---
title: Évaluer et comparer des agents
resume: Des mesures (tokens, coût, exactitude, score) plutôt qu'une impression.
cm: CM2 — Un essai ne prouve rien ; Un critère de choix ; CM3 — Comparer deux architectures
td: td3, td5, td6
---
Un agent est non déterministe : une exécution ne prouve rien. Pour comparer deux architectures :

- fixez ce qui doit l'être (graine du jeu, mêmes cas, même modèle) ;
- relevez des **mesures** : exactitude, tokens, latence, coût, score du jeu ;
- rejouez plusieurs fois, notez la **dispersion** ;
- **lisez les traces** pour comprendre *pourquoi* une architecture fait mieux ;
- comparez à une **référence** (score humain au TD6, workflow classique au TD5).

Une conclusion honnête sait dire « pas de différence nette sur ces essais ».
