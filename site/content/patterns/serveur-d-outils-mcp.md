---
title: Serveur d'outils (MCP)
aka: Tool provider, Model Context Protocol
famille: Interopérabilité
niveau: 2
cout: faible
controle: le serveur publie, le client découvre
resume: Un programme externe publie ses outils par un protocole standard ; l'agent les découvre et les appelle comme les siens.
probleme: Brancher des outils venus d'ailleurs sans réécrire l'agent.
td: td6
notions: mcp, profils-secrets, texte-tiers-injection, appel-outils
recettes: client-mcp, se-proteger-d-un-texte-piege
---
## Intention
Découpler l'**agent** des **outils** : n'importe quel serveur MCP peut offrir des outils à n'importe quel client MCP.

## Problème
Chaque application expose ses fonctions à sa façon. Écrire un adaptateur par application ne passe pas à l'échelle.

## Structure
{{fig:mcp}}

## Participants
| Participant | Rôle |
|---|---|
| Serveur MCP (le jeu) | publie ses outils (noms, descriptions, paramètres) |
| Client MCP (la clinique) | découvre et appelle |
| `ToolCallback` | forme sous laquelle l'agent voit chaque outil |

## Dynamique
1. Connexion → *List tools*.
2. Les outils découverts deviennent des `ToolCallback`.
3. L'agent les utilise comme n'importe quel autre outil.

## Conséquences
- Votre code d'agent **ne change pas**.
- Un écosystème d'outils réutilisables.
{: .plus }

- Vous faites **confiance** aux descriptions et aux résultats du serveur.
- Dépendance au serveur (disponibilité, latence).
{: .moins }

## Quand l'utiliser
Quand les outils sont fournis par un **autre programme** ou une autre équipe.

## Dans la clinique
TD6 : le jeu du bestiaire expose huit outils sur `/mcp` ; le client est activé par le profil `jeu`.

## Mesurer
Outils découverts, latence par appel, erreurs de connexion ; et, côté sécurité, ce que l'agent peut faire avec chaque outil.
