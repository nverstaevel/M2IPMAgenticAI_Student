---
title: « Proposer, jamais écrire » : la confirmation humaine
resume: Le modèle prépare une action d'écriture ; seul un humain, par un autre chemin, la valide.
cm: CM1 — Confirmation des écritures ; CM2 — La proposition d’abord ; Écrire dans l’agenda : proposer, puis confirmer
td: td2, td3, td4, td5
famille: outils
ordre: 2
---
## L'idée
Un modèle peut mal comprendre, être trompé par un texte piégé, ou inventer un identifiant. On ne lui donne donc **jamais** le pouvoir d'écrire seul : il **propose**, un humain **confirme**.

## Comprendre
{{fig:proposer_confirmer}}

1. Un outil `proposer…` **vérifie** les données (la créature existe ? le créneau est libre ?) et enregistre une **proposition** (identifiant, durée de validité). Il n'écrit rien.
2. L'interface affiche la proposition avec *Confirmer* / *Annuler*.
3. *Confirmer* appelle un **endpoint distinct**, réservé au personnel, que **le modèle ne peut pas appeler**.

C'est la **frontière** qui compte : ce n'est pas une consigne du prompt (« n'écris pas sans demander »), c'est une impossibilité technique.

## Dans la clinique
Le même principe vaut pour la base (TD2), pour Google Agenda (TD3), pour les agents (TD4, TD5) et pour l'application d'un traitement (validation humaine du dossier, TD5).

## À retenir
- Le modèle prépare ; l'humain valide ; le service métier exécute.
- La proposition expire et ne s'applique qu'une fois.

## Erreurs fréquentes
- Se fier à l'identifiant de proposition écrit dans le **texte** du modèle : il vient de l'interface.
- Oublier l'expiration : une proposition vieille d'un jour ne doit plus s'appliquer.

## Auto-test
Q: Pourquoi « n'écris jamais sans confirmation » dans le prompt ne suffit-il pas ?
R: Un prompt est une consigne, pas une barrière : un texte piégé ou une erreur du modèle peut la contourner. La barrière doit être technique.

Q: Que vérifier quand on confirme deux fois la même proposition ?
R: La seconde doit être refusée : la proposition est supprimée après usage.
