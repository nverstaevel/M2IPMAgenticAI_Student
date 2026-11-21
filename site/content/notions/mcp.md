---
title: Le protocole MCP (Model Context Protocol)
resume: Un standard pour qu'un agent découvre et appelle des outils exposés par un autre programme.
cm: CM3 — MCP en un tableau ; Ce que le serveur MCP publie ; Deux applications, un protocole
td: td6
---
Un **serveur MCP** expose des outils (le jeu les expose sur `http://localhost:8090/mcp`). Un **client MCP** (votre clinique) les **découvre** à la connexion et les appelle.

Ce que vous devez retenir : **un outil MCP distant est un `ToolCallback` comme un autre**. Votre code agent ne change pas ; seul le point de départ des outils change.

- Transport : *Streamable HTTP*.
- Pour explorer sans écrire de code : `npx @modelcontextprotocol/inspector`, puis *List Tools*.
- Démarrez le jeu **avant** la clinique ; sans profil `jeu` (ou sans serveur), `connecte: false`.
