# M2IPMAgenticAI — Dépôt étudiants

Code fourni pour les TD « IA agentiques avec Spring AI » (M2 MIAGE IPM, Université Toulouse Capitole). Fil rouge : la clinique des créatures fantastiques du Dr Gribouille.

| Dossier | TD | Ce que vous faites |
|---|---|---|
| `td1-clinique` | TD1 | Application Spring Boot classique (JPA, services, API REST, pages) |
| `td2-chat` | TD2 | Chat interrogeant l'application (`ChatClient`, mémoire, outils) |
| `td3-outils-externes` | TD3 | Wikipédia, Excel, base externe, agenda, expérience de contexte |
| `td4-agent-react` | TD4 | Boucle ReAct écrite à la main, traces |
| `td5-agents-roles` | TD5 | Agents à rôles, scénarios `.md`, dossier PDF |
| `td6-mcp-jeu` | TD6 | Client MCP : Gribouille joue au jeu `jeu-bestiaire` |
| `jeu-bestiaire` | TD6 | Le jeu (serveur MCP), fourni, à ne pas modifier |

**Chaque dossier `tdN-…` est le point de départ du TD N** : il contient le corrigé du TD précédent, le code fourni du TD, et des squelettes (`À écrire`) pour ce que vous devez coder. Travaillez dans votre propre copie (fork ou clone), un dossier à la fois.

- JDK 21 requis. Ouvrez **le dossier d'un TD** (pas la racine) dans Spring Tools / IntelliJ : `File > Import > Maven > Existing Maven Projects`.
- Chaque TD s'exécute avec `mvn spring-boot:run` ; les profils de modèle (`lmstudio`, `groq`, `gemini`, `claude`, `simule`) se choisissent avec `-Dspring-boot.run.profiles=…`.
- Les tests fournis dans un squelette échouent tant que le TD n'est pas fait : c'est normal.
- Le sujet de chaque TD est distribué en cours ; ce dépôt ne contient aucune solution.
