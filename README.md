# M2IPMAgenticAI — Dépôt étudiants

📚 **Site d'aide aux TD** (sujets, indices progressifs, notions du cours, recettes, dépannage) : <https://nverstaevel.github.io/M2IPMAgenticAI_Student/>

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
## Comment lire un dossier de TD

- **Fourni** : classes déjà écrites et testées, à lire mais pas à réécrire (la liste exacte est dans le `README.md` du dossier).
- **À écrire** : classes ou méthodes à compléter. Cherchez les marqueurs `// TODO TDn` (dans Eclipse : vue *Tasks*, dans IntelliJ : fenêtre *TODO*).
- **Corrigé du TD précédent** : le reste du code est la solution du TD N-1, pour que chacun reparte d'une base saine.
- Les tests des squelettes échouent tant que les `TODO` ne sont pas traités : c'est normal. La CI GitHub ne fait que compiler.

## Avant de lancer

- Un modèle de langage : en local (LM Studio, Ollama) ou par API (Groq, Gemini, Claude) via un profil Spring ; sans modèle, le profil `simule` permet de faire tourner les tests. Les clés d'API ne vont jamais dans Git (variables d'environnement ou `application-local.yml`, déjà ignoré).
- Tenez un `JOURNAL.md` dans votre dépôt : 5 à 10 lignes par TD ; il sert à l'analyse critique du TD6 et à l'évaluation de TD.
- **TD6** : lancez d'abord le jeu (`mvn spring-boot:run` dans `jeu-bestiaire`, port 8090), puis la clinique avec le profil `jeu`.
- Le sujet de chaque TD, les supports de CM et la matrice de compétences sont distribués en cours.
