---
title: Proposer, puis confirmer (jamais écrire directement)
resume: L'outil prépare une proposition expirante ; un endpoint réservé au personnel l'applique.
td: td2, td3, td4, td5
famille: outils
ordre: 2
notions: confirmation-ecritures, securite-basique, appel-outils, couches-spring
---
## Le schéma
{{fig:proposer_confirmer}}

## Les cinq choses à faire
1. L'outil `proposer…` **valide** les données (la créature existe ? le créneau est libre ?) et **n'écrit rien** dans la base.
2. Il enregistre une `Proposition` (identifiant, contenu, date d'expiration) et renvoie un **résumé** au modèle.
3. L'interface affiche la proposition avec Confirmer / Annuler.
4. `POST …/confirmer` est réservé au rôle `PERSONNEL`, vérifie que la proposition **existe et n'a pas expiré**, appelle le service métier, puis supprime la proposition.
5. **Aucun outil** ne permet au modèle de confirmer.

## À tester
- confirmer deux fois la même proposition → la seconde est refusée ;
- confirmer après expiration → refusé ;
- conflit de créneau → le message d'erreur du service remonte jusqu'à la conversation.

## Pièges
- Mettre l'identifiant de proposition dans la réponse du modèle et lui faire confiance : c'est l'interface qui porte l'identifiant, pas le texte généré.
- Oublier l'expiration : une proposition vieille d'un jour ne doit plus s'appliquer.
