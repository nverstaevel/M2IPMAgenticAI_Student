---
title: Acteur–critique
aka: Generator–verifier, reflection, reviewer loop
famille: Plusieurs agents
niveau: 2
cout: élevé (2 agents × tours)
controle: un agent vérifie un autre, le code borne
resume: Un agent propose, un second vérifie avec d'autres sources et peut rejeter ; la boucle est bornée.
probleme: Réduire les erreurs d'un agent en les faisant relire.
td: td5, td6
notions: agents-roles, sortie-structuree, moindre-privilege, confirmation-ecritures
recettes: duo-acteur-critique, ecrire-un-scenario, donner-ses-outils-a-un-role
---
## Intention
Attraper les erreurs d'un agent par un **second regard indépendant**.

## Problème
Un agent peut proposer un traitement contre-indiqué en toute confiance. Seul, il ne se corrige pas.

## Structure
{{fig:acteur_critique}}

## Participants
| Participant | Rôle |
|---|---|
| Acteur (vétérinaire) | produit un `Diagnostic` |
| Critique (gardien) | vérifie avec **d'autres sources**, rend un verdict structuré |
| Boucle | renvoie les objections, `max` tours |
| Humain | valide l'issue |

## Dynamique
1. L'acteur propose.
2. Le critique vérifie (Wikipédia, bestiaire) et rend `APPROUVE`, `DOUTE` ou `REJETE` avec objections et sources.
3. `REJETE` → retour à l'acteur avec les objections (au plus `max` fois).
4. L'issue part à l'humain.

## Conséquences
- Les erreurs **détectables par une autre source** sont attrapées.
- Le verdict et les objections sont **traçables**.
{: .plus }

- Deux agents, plusieurs tours : tokens × 2 ou plus.
- Un critique sans source indépendante valide ce qu'il lit.
- Faux rejets possibles.
{: .moins }

## Quand l'utiliser
Quand une erreur coûte cher **et** qu'une vérification indépendante existe.

## Quand l'éviter
Quand aucune source indépendante n'existe : le critique ne fait que répéter l'acteur.

## Variantes
- Plusieurs critiques spécialisés (sécurité, dose, espèce).
- Critique **humain** seulement (le pattern *Confirmation humaine*).

## Dans la clinique
TD5 : vétérinaire / gardien, profils prudent et pressé, cas piégés. TD6 : le gardien relit le plan avant l'apothicaire.

## Mesurer
Rejets corrects, faux rejets, faux accords, tours moyens, tokens — **comparés au workflow classique**.
