---
title: Proposer, puis confirmer
aka: Human-in-the-loop, propose-then-commit, two-phase action
famille: Humain dans la boucle
niveau: 1
cout: faible
controle: le modèle propose, l'humain décide
resume: Le modèle prépare une écriture ; seul un humain, par un chemin séparé, la valide.
probleme: Empêcher un modèle d'écrire seul.
td: td2, td3, td5
notions: confirmation-ecritures, securite-basique, appel-outils
recettes: proposer-confirmer, securiser-api
---
## Intention
Garder un **humain** responsable de toute écriture, sans priver l'assistant de sa souplesse.

## Problème
Un modèle peut mal comprendre, être trompé par un texte piégé ou inventer un identifiant. Une écriture erronée est coûteuse, parfois irréversible.

## Structure
{{fig:proposer_confirmer}}

## Participants
| Participant | Rôle |
|---|---|
| Outil `proposer…` | vérifie, enregistre une proposition, n'écrit rien |
| Proposition | contenu + identifiant + expiration |
| Interface | affiche *Confirmer* / *Annuler* |
| Endpoint de confirmation | réservé au rôle `PERSONNEL`, hors d'atteinte du modèle |
| Service métier | exécute réellement |

## Dynamique
1. Le modèle appelle `proposer…`.
2. L'outil **valide** (existence, règles) et crée la proposition.
3. L'humain voit un résumé et clique.
4. L'endpoint vérifie existence et expiration, appelle le service, **supprime** la proposition.

## Conséquences
- Une erreur du modèle ne peut pas écrire.
- La barrière est **technique**, pas une consigne de prompt.
{: .plus }

- Un clic de plus pour l'utilisateur.
- Il faut gérer l'expiration et le double clic.
{: .moins }

## Quand l'utiliser
Toute écriture dont une erreur coûterait cher : base, agenda, commande, traitement.

## Quand l'éviter
Pour des actions sans conséquence et réversibles à coût nul (lecture, brouillon).

## Variantes
- **Validation du dossier** (TD5) : l'humain valide un dossier complet produit par plusieurs agents.
- **Aperçu / appliquer** : le modèle montre un diff, l'humain l'applique.

## Mesurer
Propositions confirmées / annulées / expirées ; erreurs interceptées avant écriture.
