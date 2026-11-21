---
title: Évaluer et comparer des agents
resume: Des mesures (tokens, coût, exactitude, score) plutôt qu'une impression.
cm: CM2 — Un essai ne prouve rien ; Un critère de choix ; CM3 — Comparer deux architectures
td: td3, td4, td5, td6
---
## L'idée
Un agent est non déterministe : **une exécution ne prouve rien**. On compare des architectures avec des mesures, pas avec une impression.

## Comprendre
{{fig:evaluation}}

1. **Fixer** ce qui doit l'être : graine du jeu, mêmes cas, même modèle.
2. **Mesurer** : exactitude, tokens, latence, coût, score.
3. **Rejouer** plusieurs fois : noter la dispersion.
4. **Lire les traces** pour comprendre *pourquoi*.
5. **Comparer à une référence** : score humain (TD6), workflow classique (TD5).

Une conclusion honnête sait dire : « pas de différence nette sur ces essais ».

## À retenir
- Les mêmes conditions, sinon on compare des choses différentes.
- Dispersion + traces = ce qui rend une conclusion crédible.

## Erreurs fréquentes
- Conclure sur un seul essai.
- Changer deux choses à la fois (modèle **et** architecture).

## Auto-test
Q: A obtient 120 points, B 110, sur une partie chacune. Conclusion ?
R: Aucune : il faut rejouer, comparer la dispersion, lire les traces. Une partie ne distingue pas A de B.
