# Sujets de TD — IA agentiques avec Spring AI (M2 MIAGE IPM)


## Organisation

Six TD de 3 h, faits seul ou à deux (comme vous préférez), construisent une même application sur tout le semestre : la clinique des créatures fantastiques et son assistant, le Dr Gribouille. Chaque TD part du résultat du précédent. Si vous êtes en retard, vous récupérez un dossier de reprise (le dossier `tdN-…` du TD précédent, fourni par l'enseignant) au début de la séance suivante.

Ordre des séances : CM1, TD1, TD2, CM2, TD3, TD4, TD5, CM3, TD6, CM4. Le CM2 couvre les sources externes, la gestion du contexte et la boucle d'agent (ReAct) : il prépare TD3, TD4 et TD5. Le CM3 présente le protocole MCP et le jeu de la clinique (architecture, confiance, évaluation) et prépare le TD6.

| TD | Thème | Ce que vous livrez |
| --- | --- | --- |
| TD1 | Application Spring Boot classique | La clinique, sans IA |
| TD2 | Un chat qui interroge l'application | Le Dr Gribouille et ses outils internes |
| TD3 | Outils externes et contexte | Quatre outils et un tableau de mesures |
| TD4 | Agent ReAct | Une boucle d'agent et ses traces commentées |
| TD5 | Agents à rôles | Un duo vétérinaire / gardien du bestiaire et un dossier PDF |
| TD6 | Le jeu de la clinique en MCP | Des architectures d'agents qui jouent, comparées par leurs scores |

Les TD ne sont pas notés, mais l'évaluation de TD (CM4, 1 h sur machine) porte sur ce que vous y aurez appris : corriger et câbler un outil, analyser une trace d'agent, rédiger une courte analyse critique. Vos livrables sont donc votre préparation.

## Comment est construit chaque sujet

Tous les sujets suivent le même plan, pour que vous puissiez les faire en autonomie :

| Section | Ce que vous y trouvez |
| --- | --- |
| Objectifs de la séance | ce que vous saurez faire à la fin, et le critère « Vous avez réussi si… » |
| Vous écrivez / Fourni | la répartition entre votre code et le dossier de départ |
| Avant la séance | une liste à cocher : installation, fichiers, relectures du cours |
| Ressources | les supports de cours, documentations et exemples, avec l'étape où les utiliser |
| Parcours guidé | un tableau d'étapes avec durées et points de contrôle, puis le détail de chaque étape avec exemples de code |
| En cas de blocage | symptôme, cause fréquente, que faire |
| Questions de réflexion, Livrable, Pour aller plus loin | ce qui prépare l'évaluation de TD |

Un **point de contrôle** est une vérification que vous pouvez faire seul (une commande, une page, un test) avant de passer à l'étape suivante. Si un point de contrôle échoue, ne continuez pas : reportez-vous à « En cas de blocage », puis demandez de l'aide. Les extraits de code sont des repères : la documentation de Spring AI fait foi pour la version fournie.

## Outils communs

- **Fournisseur de modèle.** Du TD2 au TD6, choisissez un fournisseur (Ollama ou LM Studio en local, ou une API : Claude, Gemini, Groq) par un profil Spring. Le tableau et les commandes sont au TD2. Le contrôle est toujours le même : `curl -u gribouille:gribouille localhost:8080/api/diagnostic/modele` doit répondre `"statut":"OK"`.
- **Page `/agents`** (à partir du TD4). Elle dessine le graphe d'exécution d'un agent : étapes, appels d'outils, retours, tokens, durée. Elle sert à lire une trace, à comparer ce qui était prévu et ce qui s'est passé, et à rejouer un scénario. Ouvrez-la dès que vous voulez comprendre pourquoi un agent a fait ce qu'il a fait.
- **Scénarios en Markdown** (TD5, TD6). Une architecture d'agents se décrit dans un fichier `.md` du dossier `scenarios/` : trois sections : Entrées, Agents (beans Java et outils autorisés) et Graphe, dessiné avec des flèches (a -> b, boucles avec max). `SCENARIOS.md` explique le format et `scenarios/_modele.md` est un point de départ à copier.
- **Suivi de l'exécution.** Les classes `SuiviExecution` et `SuiviOutils` du dossier fourni enregistrent les étapes, les tokens et les erreurs. Un outil appelé plus de 15 fois dans une même exécution est coupé : c'est un garde-fou, pas un bug.

## Organisation du code, Git, conventions et journal de bord

- Un dossier de travail par TD (`td1-clinique`, `td2-chat`, `td3-outils-externes`, `td4-agent-react`, `td5-agents-roles`, `td6-mcp-jeu`), où vous voulez sur votre machine. Au début de chaque TD, prenez le dossier de départ fourni par le dépôt M2IPMAgenticAI\_Student (corrigé du TD précédent, code fourni, squelettes « À écrire »), ou votre dossier précédent en y ajoutant le code fourni, et travaillez dans la copie ; le dossier jeu-bestiaire (le jeu du TD6) est fourni tel quel et ne se modifie pas.
- **Git est recommandé, pas obligatoire.** Il vous donne un historique et une sauvegarde (un dépôt personnel ou à deux, si vous voulez), mais vous pouvez très bien télécharger le dépôt fourni en ZIP et travailler sans. Dans tous les cas, gardez une copie de votre travail à la fin de chaque séance.
- Les versions de Java, Spring Boot et Spring AI sont fixées dans le `pom.xml` du squelette. Ne les modifiez pas.
- Packages : `domain`, `repository`, `service`, `controller`, `dto`, `ai` (chat, outils, agents), `externe` (Wikipédia, Excel, catalogue, agenda), `agent` (boucle, traces), `experience`, `roles` et `scenario`, `jeu` (client MCP).
- Chaque service public a au moins un test. Aucun test n'appelle un vrai modèle : utilisez le modèle simulé fourni.
- Tenez un fichier `JOURNAL.md`. À chaque TD, notez 5 à 10 lignes : ce qui a marché, ce qui vous a surpris, une réponse absurde du modèle (copiée telle quelle), une décision prise et pourquoi.

Le journal sert à deux moments : l'analyse critique du TD6 et la préparation de l'évaluation de TD.

## Règles de sécurité

- **Secrets.** Clés d'API et fichier du compte de service Google ne vont jamais dans Git. Utilisez des variables d'environnement ou un fichier `application-local.yml`, déjà ignoré par le `.gitignore` fourni.
- **Données.** Uniquement des données fictives. N'écrivez jamais de donnée personnelle réelle dans un prompt, surtout vers un modèle hébergé.
- **Agenda.** Créez votre propre agenda de test (un agenda vide, dédié à la séance) et n'utilisez que lui. Aucune invitation n'est envoyée et aucun autre agenda n'est modifié.
- **Budget.** Un modèle hébergé a un plafond de dépense. Surveillez les tokens consommés dès le TD2.
- **Actions d'écriture.** Le modèle ne décide jamais seul d'écrire en base ou dans l'agenda : une confirmation passe par un endpoint distinct, que le modèle ne peut pas appeler (introduit au TD2).
- **Assistants IA.** Libres en TD, pour apprendre. Notez dans le journal ce que vous avez demandé et ce que vous avez vérifié. Interdits pendant les évaluations.
