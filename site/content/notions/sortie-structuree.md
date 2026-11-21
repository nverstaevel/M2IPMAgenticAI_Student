---
title: Sortie structurée
resume: Texte libre en entrée, objet Java en sortie : `.entity(Record.class)`.
cm: CM1 — ChatClient en quelques lignes ; CM2 — Un modèle en critique un autre
td: td2, td5
---
Au lieu de parser une phrase, demandez au modèle de remplir un `record` : `client.prompt().user(texte).call().entity(Triage.class)`. Spring AI ajoute au prompt le schéma attendu et convertit la réponse.

- Utilisez un client **séparé**, sans mémoire ni outils : un appel isolé est plus fiable.
- Une `enum` limite les valeurs possibles (`FAIBLE`, `MOYENNE`, `ELEVEE`).
- Le modèle peut quand même se tromper de format : prévoyez le cas d'erreur (un nouvel essai, un message clair).
- Au TD5, les agents répondent ainsi (`Diagnostic`, `Critique`) : c'est ce qui permet de les chaîner.
