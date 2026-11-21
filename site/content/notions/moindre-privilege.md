---
title: Moindre privilège : chaque rôle ses outils
resume: Donner à chaque agent seulement les outils utiles, et séparer lecture et écriture.
cm: CM2 — Un superviseur qui délègue ; CM3 — Brancher un serveur MCP, c’est lui faire confiance
td: td5, td6
famille: agents
ordre: 6
---
## L'idée
Chaque agent ne reçoit que **les outils dont il a besoin**. Moins un agent peut faire, moins une erreur ou une consigne piégée coûte cher.

## Comprendre
{{fig:roles_outils}}

| Question à se poser pour chaque rôle | Exemple |
|---|---|
| Que doit-il **lire** ? | le diagnosticien lit l'état et le dossier |
| Que doit-il **écrire** ? | seulement l'apothicaire exécute |
| Lit-il un texte de **tiers** ? | alors il ne doit avoir **aucun** outil d'écriture |
| Qui **valide** ? | le gardien vérifie, puis un humain confirme |

C'est la version « agents » de la séparation lecture / écriture : on rend certaines erreurs **impossibles** plutôt qu'improbables.

## À retenir
- Peu d'outils par agent = prompt plus court, choix d'outil plus fiable, surface d'attaque réduite.
- Lecture et écriture ne vont pas dans le même rôle quand un texte tiers est lu.

## Erreurs fréquentes
- Donner « tous les outils » à un agent pour gagner du temps : la séparation des rôles disparaît.
- Protéger seulement par le prompt (« n'écris jamais ») : une consigne n'est pas une barrière.
- Oublier qu'un outil d'écriture caché dans un groupe d'outils « de lecture » annule la séparation.

## Auto-test
Q: Pourquoi séparer le diagnosticien (qui lit les notes) de l'apothicaire (qui écrit) ?
R: Si une note piégée trompe le diagnosticien, il n'a aucun outil pour agir : le dégât reste limité à une mauvaise réponse.
