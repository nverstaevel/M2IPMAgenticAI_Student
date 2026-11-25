Les outils fournis pour **voir ce que font vos agents** : à utiliser dès le TD4, pour déboguer, comparer et alimenter votre `JOURNAL.md`. Ils sont dans le dossier du TD 6 (et déjà, pour `/chat` et `/agents`, dans les TD précédents). Tous tournent dans l'application de la clinique : `http://localhost:8080`. Connexion : `gribouille` / `gribouille` (rôle PERSONNEL) ou `visiteur` / `visiteur` (lecture seule : `/chat` et les lectures de l'API, mais ni `/agents`, ni `/pilotage`, ni `/dossiers`, réservés au personnel).

## Vue d'ensemble

| Outil | Adresse | Depuis | À quoi il sert |
|---|---|---|---|
| Chat de Gribouille | `/chat` | TD2 | Parler au modèle, voir ses appels d'outils, les propositions à confirmer |
| Agents en direct | `/agents` | TD4 | Lancer un agent ou un scénario, voir son graphe se construire, rejouer une exécution |
| Dossiers à valider | `/dossiers` | TD5 | Valider ou refuser (humain dans la boucle) le résultat d'un duo ou d'un superviseur |
| Pilotage du jeu | `/pilotage` | TD6 | Lancer une partie, voir en direct ce que décide le contrôleur, régler la vitesse |
| Le jeu | `http://localhost:8090` | TD6 | Jouer soi-même, voir l'état de la clinique, le carnet de soins |
| Serveur MCP du jeu | `http://localhost:8090/mcp` | TD6 | Les 9 outils du jeu, pour votre client MCP ou l'Inspector |

## Agents en direct : `/agents`

![Page Agents en direct : un duo vétérinaire / gardien après exécution](img/outils/agents.png)

**À quoi ça sert.** Chaque exécution d'agent (ReAct, scénario, duo, superviseur) publie ses étapes : le graphe se dessine **pendant** l'exécution. Un nœud = une étape (appel du modèle, outil, code Java, validation humaine, résultat). C'est votre instrument pour répondre à « qu'a fait l'agent, et pourquoi ? ».

**Utilisation.**
1. Choisissez un *Type d'agent* (ReAct, scénario…), un *Exemple* (il remplit la demande), puis **Lancer et suivre**.
2. Cliquez un nœud : le panneau de droite donne le détail (demande envoyée, résultat, durée, jetons).
3. *Exécutions* liste les runs passés (en mémoire) ; **Ouvrir** les recharge, **Rejouer** les repasse pas à pas.
4. *Aperçu du graphe* dessine un scénario sans l'exécuter : pratique pour vérifier vos `.md` avant de payer des appels.

| Couleur | Genre de nœud |
|---|---|
| gris | Demande de l'utilisateur ; code Java sans modèle |
| violet | Appel du modèle |
| orange | Appel d'outil |
| bleu / rouge / vert | Agent / critique / travailleur |
| jaune (pointillé) | Validation humaine en attente |
| vert foncé | Résultat |

**Interface programmable** (utile pour vos tests ou `curl`, ajoutez `-u gribouille:gribouille`) :

| Requête | Effet |
|---|---|
| `GET /api/agent/scenarios` | Liste des scénarios `.md` chargés et leur validité |
| `GET /api/agent/scenarios/{id}` | Détail d'un scénario (graphe prévu) |
| `POST /api/agent/scenarios/{id}/lancer` | Lance le scénario, répond avec le `runId` |
| `GET /api/agent/suivi` | Liste des exécutions |
| `GET /api/agent/suivi/{runId}` | Tous les événements d'une exécution (JSON) |
| `GET /api/agent/suivi/{runId}/flux` | Les mêmes événements en direct (SSE, événement `evt`) |
| `GET /api/agent/{runId}/trace` | Trace écrite sur disque (dossier `traces/`) |

Les liens `/agents?run=<runId>` s'ouvrent directement sur une exécution : collez-les dans votre journal.

## Pilotage du jeu : `/pilotage`

![Page Pilotage du jeu : paramètres, indicateurs, actions du contrôleur et jeu en direct](img/outils/pilotage.png)

**À quoi ça sert.** Faire jouer une partie complète à un « contrôleur » et **regarder** ses réflexions et ses appels d'outils pendant que le jeu évolue à droite. Le contrôleur peut être :
- **Règles fixes (sans modèle)** : un joueur en Java pur, la référence à battre. Aucun appel au modèle, résultat reproductible ;
- **un de vos scénarios** (`jeu-*.md`) : vos agents jouent avec le modèle du profil actif.

**Prérequis.** `jeu-bestiaire` démarré (port 8090) et la clinique lancée avec **deux profils** : le modèle **et** `jeu`, par exemple `groq,jeu` (`-Dspring.profiles.active=groq,jeu` si votre IDE n'accepte qu'un champ). Sinon un bandeau rouge l'indique. Classe principale : `fr.m2ipm.clinique.CliniqueApplication`.

**Utilisation.**
1. *Joueur* : règles fixes ou un scénario. *Graine* : même graine = mêmes patients, seul le joueur change (comparaison honnête).
2. **Lancer la partie** ; **Arrêter après la journée en cours** pour interrompre proprement ; **⛔ Couper tout** (kill switch) pour stopper l'agent immédiatement : plus aucun outil du jeu ne s'exécute et la fin de journée n'est pas forcée. Un appel au modèle déjà parti se termine seul, sans suite.
3. Curseur **Vitesse** : pause entre deux actions du joueur à règles, ou entre deux journées pour un scénario. Modifiable pendant la partie.
4. Section 3 : fil des actions du contrôleur (🔧 outil, ⚙️ règle, 🧑 demande…) ; chaque journée est un run, conservé dans la liste *Journées* et ouvrable dans `/agents` pour le graphe complet.
5. Section 2 : indicateurs du jeu (jour, points d'action, trésorerie, guéris, morts, score) et bilan final.

| Requête | Effet |
|---|---|
| `GET /api/agent/jeu/pilotage/options` | Joueurs disponibles, jeu connecté ou non, profils actifs |
| `GET /api/agent/jeu/pilotage/etat` | État de la partie pilotée + état du jeu |
| `POST /api/agent/jeu/pilotage/demarrer?scenario=…&graine=…&pauseMs=…` | Lance une partie |
| `POST /api/agent/jeu/pilotage/pause?ms=…` | Règle la pause |
| `POST /api/agent/jeu/pilotage/arreter` | Demande l'arrêt après la journée |
| `POST /api/agent/jeu/pilotage/couper` | Kill switch : arrêt immédiat |
| `GET /api/agent/jeu/outils` · `POST /api/agent/jeu/partie` | Outils exposés par le jeu · lancement d'une partie sans interface |

## Le jeu : `http://localhost:8090`

![Le jeu : clinique, patients, pharmacie, carnet de soins](img/jeu/jeu-vue-ensemble.png)

Lancez `jeu-bestiaire` (`fr.m2ipm.jeu.JeuApplication`, port 8090). Vous pouvez y jouer à la main : c'est la meilleure façon de comprendre les règles avant d'écrire un agent. L'interface a dix zones (état, patients, dossier, pharmacie, **carnet de soins**…) décrites dans le [sujet du TD 6](td/td6.html). Score = 100 × guérisons + trésorerie + 2 × réputation − 150 × morts.

## Serveur MCP du jeu : `http://localhost:8090/mcp`

Transport **Streamable HTTP**. Pour l'explorer sans coder : `npx @modelcontextprotocol/inspector`, transport *Streamable HTTP*, URL ci-dessus, **List Tools**. Votre clinique s'y connecte seule avec le profil `jeu`.

| Outil | Coût (points d'action) | Rôle |
|---|---|---|
| `etat_clinique` | gratuit | Jour, points restants, trésorerie, réputation, guérisons, morts, score |
| `lister_patients` | gratuit | Patients présents : PV, urgence, symptômes, signe distinctif (après examen) |
| `fiche_espece` | gratuit | Maladies d'une espèce, signe distinctif, remède |
| `carnet_soins` | gratuit | Effets de soins déjà observés dans la partie (guérison, toxique…) |
| `consulter_pharmacie` | gratuit | Prix, stock et quantité en commande |
| `examiner_patient` | 1 point | Révèle le signe distinctif qui tranche entre deux maladies |
| `soigner` | 1 point | Administre un médicament : bon remède = guérison, toxique = dégâts |
| `commander` | gratuit | 1 à 20 unités, payées tout de suite, livrées en fin de journée |
| `fin_de_journee` | — | Passe au lendemain : patients non soignés perdent 15 PV, livraisons, nouveaux patients |

## Les outils internes de la clinique (appelés par vos agents)

Ce sont des méthodes Java annotées `@Tool` : le modèle les voit par leur **nom** et leur **description**, rien d'autre. Un nom ou une description flou, et l'agent se trompe d'outil.

| Classe (TD) | Outils |
|---|---|
| `OutilsClinique` (TD2) | `rechercherProprietaires`, `trouverProprietaire`, `creaturesDuProprietaire`, `consultationsDeLaCreature`, `soigneursPourEspece`, `proposerConsultation` (propose seulement : le personnel confirme) |
| `OutilsExternes` (TD3) | `chercherDansWikipedia`, `consulterTarif` et `stockPharmacie` (fichier Excel), `chercherMedicament` (catalogue), `creneauxLibres` et `proposerEvenementAgenda` (agenda) |
| `OutilsBestiaire`, `OutilsPharmacie`, `OutilsAgenda` (TD5) | les mêmes, répartis par **rôle** pour que chaque agent n'ait que les outils de son métier |
| Jeu MCP (TD6) | les 9 outils du tableau ci-dessus, découverts à la connexion |

Les deux outils « à effet » (`proposerConsultation`, `proposerEvenementAgenda`) **ne modifient jamais rien** : ils créent une proposition que `/chat` ou `/dossiers` fait valider par un humain.

## Si ça ne marche pas

| Symptôme | Cause probable |
|---|---|
| `/pilotage` : « le jeu n'est pas connecté » | `jeu-bestiaire` non démarré, ou clinique lancée sans le profil `jeu` |
| `ClassNotFoundException … JeuApplication` | Mauvais dossier ouvert dans l'IDE : `JeuApplication` est dans `jeu-bestiaire`, pas dans le TD 6 |
| 401 / 403 | Identifiants absents (`-u gribouille:gribouille`), ou compte `visiteur` sur une page ou une action réservée au personnel |
| Erreur « tool … not in request.tools » (Groq) | Le modèle a inventé un nom d'outil : l'agent réessaie 3 fois, comparez avec les noms exacts ci-dessus |
| Graphe vide dans `/agents` | Exécution perdue au redémarrage (les runs sont en mémoire) : relancez ou rouvrez la trace dans `traces/` |

Plus de pistes : [Dépannage](depannage.html).
