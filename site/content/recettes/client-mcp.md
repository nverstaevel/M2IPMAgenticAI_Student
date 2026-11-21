---
title: Connecter la clinique au jeu (client MCP)
resume: Le starter, deux fichiers de propriétés, `JeuMcp`, un point de contrôle.
td: td6
famille: agents
ordre: 8
notions: mcp, profils-secrets, appel-outils, texte-tiers-injection
---
{{fig:mcp}}

## 1. Le jeu
```bash
mvn -f jeu-bestiaire/pom.xml spring-boot:run        # http://localhost:8090 ; MCP sur /mcp
```
Explorez les outils avec l'inspecteur : `npx @modelcontextprotocol/inspector` → *Streamable HTTP* → `http://localhost:8090/mcp` → *List Tools*.

## 2. Le client
```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-starter-mcp-client</artifactId>
</dependency>
```
```properties
# application.properties : la clinique doit démarrer sans le jeu
spring.ai.mcp.client.enabled=false

# application-jeu.properties
spring.ai.mcp.client.enabled=true
spring.ai.mcp.client.streamable-http.connections.jeu.url=http://localhost:8090
spring.ai.mcp.client.request-timeout=60s
agent.budget-tokens=200000
```

## 3. Le composant
`JeuMcp` reçoit le `ToolCallbackProvider` publié par le starter ; il expose `outils()` et `appeler(nom, json)`. **Un outil MCP est un `ToolCallback` comme un autre.**

## 4. Contrôle
```bash
curl -u gribouille:gribouille http://localhost:8080/api/agent/jeu/outils
# connecte: true, huit outils
```

## Pièges
| Symptôme | Cause |
|---|---|
| `connecte: false` | profil `jeu` absent, ou jeu pas démarré |
| la clinique ne démarre pas | jeu injoignable au démarrage avec le profil `jeu` |
| ⚠ « outil inconnu » dans un scénario | nom faux (avec underscore) ou profil absent |
| port 8090 déjà pris | ancien jeu encore lancé |
