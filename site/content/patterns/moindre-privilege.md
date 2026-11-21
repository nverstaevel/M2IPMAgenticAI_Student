---
title: Séparation des rôles et moindre privilège
aka: Least privilege, read/write split, quarantined reader
famille: Plusieurs agents
niveau: 2
cout: faible
controle: la configuration impose les droits
resume: Chaque agent reçoit seulement les outils utiles ; celui qui lit un texte tiers n'écrit jamais.
probleme: Limiter les dégâts d'une erreur ou d'un texte piégé.
td: td5, td6
notions: moindre-privilege, texte-tiers-injection, confirmation-ecritures, agents-roles
recettes: donner-ses-outils-a-un-role, se-proteger-d-un-texte-piege
---
## Intention
Rendre certaines erreurs **impossibles** plutôt qu'improbables, en limitant ce que chaque agent peut faire.

## Problème
Un agent qui lit des notes de tiers *et* peut commander est une cible : un texte piégé peut le détourner.

## Structure
{{fig:roles_outils}}

## Participants
| Participant | Rôle | Outils |
|---|---|---|
| Lecteur (diagnosticien) | lit, analyse | lecture seule |
| Vérificateur (gardien) | contrôle | sources externes, pas d'écriture |
| Exécutant (apothicaire) | agit après validation | écriture |

## Dynamique
1. Le lecteur analyse et **propose** (jamais d'effet).
2. Le vérificateur contrôle.
3. L'exécutant applique **uniquement** un plan validé.

## Conséquences
- Surface d'attaque réduite : un lecteur piégé ne peut pas écrire.
- Prompts courts, choix d'outil plus fiable.
{: .plus }

- Plus d'agents à écrire et à faire communiquer.
- Un défaut de séparation (un groupe d'outils trop large) annule le bénéfice.
{: .moins }

## Quand l'utiliser
Dès qu'un agent lit un **texte de tiers** ou que des écritures coûtent cher.

## Dans la clinique
TD6, étape 4 : séparer lecture et écriture ; le gardien recopie le plan avant que l'apothicaire n'agisse.

## Mesurer
Pièges évités sur des notes piégées ; appels d'outils d'écriture par rôle (doit être nul pour les lecteurs).
