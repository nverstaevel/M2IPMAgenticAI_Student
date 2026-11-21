---
title: Diagnostiquer une panne en cinq minutes
resume: Dans quel ordre vérifier : modèle, profil, logs, outil, test.
td: td2, td3, td4, td5, td6
notions: tester-sans-modele, traces-graphe
---
Avant de demander de l'aide, faites ce parcours et **notez le résultat** de chaque étape.

1. **L'application démarre-t-elle ?** Lisez la première erreur du log, pas la dernière.
2. **Le modèle répond-il ?** `curl -u gribouille:gribouille localhost:8080/api/diagnostic/modele` doit dire `OK`.
3. **Le bon profil est-il actif ?** Cherchez *The following profiles are active* dans le log.
4. **Que voit le modèle ?** Logs `DEBUG` Spring AI : requêtes, appels d'outils, arguments.
5. **Le problème vient-il de l'outil ou du modèle ?** Appelez la méthode de l'outil dans un test, sans modèle. Si elle marche, le souci est la description ou le modèle.
6. **Reproduisez avec le profil `simule`.** Si ça marche avec le simulé, c'est le modèle ; sinon, c'est votre code.

## Quand demander de l'aide
Apportez : le **message d'erreur exact**, la **commande** lancée, **ce que vous avez déjà essayé**. Voir la page Dépannage pour les symptômes classiques de chaque TD.
