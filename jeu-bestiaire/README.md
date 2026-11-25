# Clinique du bestiaire — le jeu (TD6)

Application Spring Boot **fournie** : à ne pas modifier. Elle simule une clinique (patients, pharmacie, trésorerie) et publie neuf outils par **MCP** (Streamable HTTP) : `etat_clinique`, `lister_patients`, `fiche_espece`, `carnet_soins`, `consulter_pharmacie`, `examiner_patient`, `soigner`, `commander`, `fin_de_journee`.

```bash
mvn spring-boot:run        # http://localhost:8090 (tableau de bord), MCP sur http://localhost:8090/mcp
```

- 5 jours, 3 arrivées par jour, 8 points d'action par jour (examiner et soigner coûtent 1 point).
- Même graine, mêmes patients : `POST /api/nouvelle-partie?graine=42`.
- Score = 100 × guérisons + trésorerie + 2 × réputation − 150 × morts.
- Explorer sans écrire de code : `npx @modelcontextprotocol/inspector` (transport Streamable HTTP, URL `http://localhost:8090/mcp`).

## La page du jeu

`http://localhost:8090` montre la clinique en direct : les créatures entrent par la porte et s'installent dans la salle d'attente, chacune avec une jauge de PV et un compte à rebours en **jours restants** (elle perd 15 PV à chaque fin de journée). Dr Gribouille commente chaque action ; la pharmacie, la trésorerie et la réputation se mettent à jour.

Un humain peut aussi **jouer à la main** : clic sur une créature (examiner, soigner), clic sur un flacon (commander), bouton « Fin de journée ». C'est votre référence humaine pour le TD6. Ces actions passent par `/api/examiner`, `/api/soigner`, `/api/commander`, `/api/fin-de-journee` ; un agent, lui, n'a que les outils MCP.
