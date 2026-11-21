---
title: Le protocole MCP (Model Context Protocol)
resume: Un standard pour qu'un agent découvre et appelle des outils exposés par un autre programme.
cm: CM3 — MCP en un tableau ; Ce que le serveur MCP publie ; Deux applications, un protocole
td: td6
famille: agents
ordre: 8
---
## L'idée
**MCP** (Model Context Protocol) est un standard pour qu'une application découvre et appelle des outils exposés par un autre programme.

## Comprendre
{{fig:mcp}}

- **Serveur MCP** : le jeu expose ses outils sur `http://localhost:8090/mcp`.
- **Client MCP** : votre clinique les **découvre** à la connexion (*List tools*), puis les appelle.
- Transport : *Streamable HTTP*.

**À retenir : un outil MCP distant est un `ToolCallback` comme un autre.** Votre code d'agent ne change pas ; seul le point de départ des outils change.

Pour explorer sans écrire de code : `npx @modelcontextprotocol/inspector`, puis *List Tools*.

## À retenir
- Démarrez le **jeu avant** la clinique.
- Sans profil `jeu`, ou sans serveur : `connecte: false`.
- Brancher un serveur MCP, c'est lui **faire confiance** (descriptions d'outils, résultats : voir *Texte écrit par un tiers*).

## Erreurs fréquentes
- Croire que MCP rend les outils sûrs : le serveur est un tiers, ses descriptions et ses résultats entrent dans le contexte du modèle.
- Démarrer la clinique avant le jeu : le client ne trouve aucun outil (`connecte: false`).
- Écrire le nom d'un outil sans underscore dans un `.md` : l'outil est « inconnu ».

## Auto-test
Q: Votre agent doit-il être réécrit pour utiliser les outils du jeu ?
R: Non : ce sont des `ToolCallback`. Seul le groupe d'outils disponible change.
