# Clinique du bestiaire — le jeu (TD6)

Application Spring Boot **fournie** : à ne pas modifier. Elle simule une clinique (patients, pharmacie, trésorerie) et publie huit outils par **MCP** (Streamable HTTP) : `etat_clinique`, `lister_patients`, `fiche_espece`, `consulter_pharmacie`, `examiner_patient`, `soigner`, `commander`, `fin_de_journee`.

```bash
mvn spring-boot:run        # http://localhost:8090 (tableau de bord), MCP sur http://localhost:8090/mcp
```

- 5 jours, 3 arrivées par jour, 5 points d'action par jour (examiner et soigner coûtent 1 point).
- Même graine, mêmes patients : `POST /api/nouvelle-partie?graine=42`.
- Score = 100 × guérisons + trésorerie + 2 × réputation − 150 × morts.
- Explorer sans écrire de code : `npx @modelcontextprotocol/inspector` (transport Streamable HTTP, URL `http://localhost:8090/mcp`).
