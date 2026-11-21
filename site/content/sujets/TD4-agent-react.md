# TD4 — Agent ReAct

## Objectifs de la séance

À la fin de la séance, vous saurez :

1. **écrire** à la main une boucle ReAct (penser, agir, observer) en appelant directement le `ChatModel` ;
2. **garantir son arrêt** par cinq motifs enregistrés (vous écrivez le plafond d'étapes et la boucle détectée ; le reste est fourni) : réponse finale, plafond d'étapes, budget de tokens, boucle détectée, erreur ;
3. **observer** une exécution en direct sous forme de graphe (page `/agents`) et la relire nœud par nœud : durée, tokens, arguments, résultat ;
4. **classer** des échecs d'agent en familles, chacune appuyée sur une trace réelle.

**Vous avez réussi si** votre agent répond au scénario C1 et si, à partir du graphe seul, vous pouvez dire pourquoi il s'est arrêté, combien d'étapes et de tokens il a coûté, et quel outil a fourni chaque information. Le but n'est pas que ça marche toujours : c'est de comprendre pourquoi ça marche parfois.

| Vous écrivez | Fourni dans le dossier de départ (ne pas modifier) |
| --- | --- |
| le cœur de `AgentReactService.executer` aux endroits marqués `TODO TD4` : appel du modèle, réponse finale, exécution des outils, historique, puis les arrêts sur plafond d'étapes et sur boucle détectée | `Etape`, `ResultatAgent`, `MotifArret`, `TraceStore`, `AgentController`, le reste de `AgentReactService` (suivi du graphe, arrêts sur budget de tokens et sur erreur), et l'outillage de suivi : `Evenement`, `SuiviExecution`, `SuiviController`, `SuiviOutils`, `Jetons`, la page `/agents` |

## Avant la séance

- [ ] Copiez `td3-outils-externes` en `td4-agent-react` (ou prenez le dossier de reprise) et travaillez dans la copie.
- [ ] Lancez l'application avec le profil de votre fournisseur, puis vérifiez le modèle : `curl -u gribouille:gribouille localhost:8080/api/diagnostic/modele` doit répondre `"statut":"OK"`. Sans modèle disponible, utilisez le profil `simule` : toutes les étapes de ce sujet restent faisables, avec des réponses déterministes.
- [ ] Ouvrez `http://localhost:8080/agents` (gribouille/gribouille). La page est vide pour l'instant : c'est normal, elle se remplira dès que votre boucle tournera (étape 1).
- [ ] Relisez dans le support de CM2 : *Penser, agir, observer*, *Quand s'arrêter*, *Lire une trace*.
- [ ] Sur papier, avant la séance : dessinez les états de la boucle (appel du modèle, exécution d'outils, fin) et qui déclenche chaque transition. Vous gagnez 15 minutes en séance.

## Ressources

| Ressource | Pour quoi faire | Quand |
| --- | --- | --- |
| Support de CM2, section « Agent ReAct » | le schéma de la boucle et les motifs d'arrêt | avant la séance |
| [ReAct : Synergizing Reasoning and Acting in Language Models](https://arxiv.org/abs/2210.03629) (Yao et al., 2022) | l'origine de l'idée ; lire l'introduction et la figure 1 | avant ou pendant l'étape 1 |
| [Spring AI : Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html) | `ToolCallingManager`, `ToolExecutionResult`, boucle pilotée par l'appelant | étapes 1 et 2 |
| [Spring AI : ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html) | la boucle du framework, pour l'étape 4 | étape 4 |
| Vos outils des TD2 et TD3 | ce que l'agent peut appeler | toute la séance |
| `JOURNAL.md` | y noter chaque exécution (étapes, motif, tokens, qualité) | à partir de l'étape 3 |

## Parcours guidé

Chaque étape se termine par un **point de contrôle** : ne passez à la suivante que s'il est vérifié.

| Étape | Durée | Vous faites | Point de contrôle |
| --- | --- | --- | --- |
| 1. Écrire la boucle | 40 min | le cœur de `AgentReactService.executer` : appel du modèle, réponse finale, exécution des outils, historique (le suivi du graphe est fourni) | « tarif d'une consultation pour un dragon » aboutit à une réponse chiffrée, et le graphe s'affiche sur `/agents` |
| 2. Deux arrêts | 20 min | plafond d'étapes et boucle détectée | les deux tests correspondants passent avec le modèle simulé |
| 3. Cinq exécutions | 30 min | C1 une fois, C2 trois fois, C3 une fois | cinq lignes dans le journal |
| 4. Le framework | 15 min | refaire C2 avec `ChatClient` | cinq lignes de comparaison |
| 5. Catalogue des échecs | 15 min | trois familles d'échec | chaque ligne cite une trace |

Le socle représente environ 2 h. Le reste de la séance est pour les extensions (section « Extensions »). Le dessin des états de la boucle se fait avant la séance.

### Avant la séance — Comprendre la boucle

Dans Spring AI 2.0, la boucle d'appel d'outils est gérée par le `ChatClient`. Pour l'écrire à la main, on appelle le `ChatModel` directement : il ne lance jamais les outils lui-même. Repère de code (indicatif, adaptez-le à la version fournie) :

```java
ToolCallingManager gestionnaire = ToolCallingManager.builder().build();
ToolCallback[] outils = ToolCallbacks.from(outilsClinique, outilsExternes);
ToolCallingChatOptions options = ToolCallingChatOptions.builder().toolCallbacks(outils).build();

Prompt prompt = new Prompt(messagesInitiaux, options);
ChatResponse reponse = chatModel.call(prompt);

while (reponse.hasToolCalls()) {
    ToolExecutionResult resultat = gestionnaire.executeToolCalls(prompt, reponse);
    prompt = new Prompt(resultat.conversationHistory(), options);
    reponse = chatModel.call(prompt);
}
String reponseFinale = reponse.getResult().getOutput().getText();
```

Cette boucle s'arrête quand le modèle ne demande plus d'outil. Rien ne garantit que ça arrive : c'est à vous d'ajouter les arrêts. Dessinez sur papier : quels sont les états (appel du modèle, exécution d'outils, fin) ? Quelles transitions ? Qui les déclenche ?

### Étape 1 — Écrire la boucle

La boucle complète doit faire ce qui suit. Le squelette fournit déjà les points 1, 2 (comptage), 3 et 4 (résultat) ; vous écrivez l'appel du modèle, la réponse finale, l'exécution des outils et l'historique, aux endroits marqués TODO TD4 (étape 1) :

1. demander dans le prompt système une phrase de raisonnement avant chaque appel d'outil (les API modernes n'exposent pas de « pensée » séparée : vous la sollicitez dans le texte, et vous la lirez avec esprit critique) ;
2. compter les étapes (une étape = un appel au modèle suivi de l'exécution des outils demandés) ;
3. passer `Map.of("conversationId", runId)` en contexte d'outil ;
4. renvoyer un `ResultatAgent` : réponse finale, motif d'arrêt, liste des `Etape`, tokens consommés.

Les outils d'écriture restent des propositions, comme au TD2 et au TD3 : l'agent ne crée rien tout seul.

Premier essai, sans modèle réel si besoin :

```bash
curl -u gribouille:gribouille -H 'Content-Type: application/json' \
  -d '{"demande":"Quel est le tarif d'\''une consultation pour un dragon ?","maxEtapes":8}' \
  localhost:8080/api/agent
```

### Étape 2 — Deux arrêts à écrire

Vous écrivez `PLAFOND_ETAPES` et `BOUCLE_DETECTEE` (marqués TODO TD4, étape 2). `REPONSE_FINALE` vient de l'étape 1 ; `BUDGET_TOKENS` et `ERREUR` sont fournis dans le squelette : lisez-les, ils servent de modèle.

| Motif | Signal à détecter | Test à écrire (modèle simulé) |
| --- | --- | --- |
| `REPONSE_FINALE` | la réponse du modèle ne contient aucun appel d'outil | une demande sans outil |
| `PLAFOND_ETAPES` | `numero >= maxEtapes` | `maxEtapes = 1` sur une demande qui exige deux outils |
| `BUDGET_TOKENS` | tokens cumulés ≥ budget | budget minuscule |
| `BOUCLE_DETECTEE` | même outil, mêmes arguments que l'étape précédente | le modèle simulé répète `outil:consulterTarif:{…}` |
| `ERREUR` | exception du modèle ou d'un outil | outil qui lève une exception |

Le test de boucle détectée se pilote avec le modèle simulé : `outil:NOM:{json}` déclenche l'appel de l'outil NOM (voir le README du TD2).

### Fourni — Le graphe de suivi

Cette partie est déjà écrite dans le squelette : lisez-la pour comprendre ce que la page `/agents` reçoit. Extension : réécrivez-la vous-même.

La page `/agents` dessine ce que vous publiez dans `SuiviExecution`. Quatre appels suffisent :

```java
suivi.demarrer(runId, "ReAct : " + abreger(demande));                       // début de l'exécution
suivi.noeud(runId, "demande", List.of(), Evenement.UTILISATEUR, "Demande",
            Evenement.OK, demande, null, null, null);                       // le nœud d'entrée

// à chaque étape numero :
String idModele = "e" + numero;
suivi.noeud(runId, idModele, List.of(precedent), Evenement.MODELE,
            "Étape " + numero + " · modèle", Evenement.EN_COURS, null, null, null, null);
ChatResponse reponse = modele.call(prompt);
Jetons j = Jetons.de(reponse, prompt, texte);                              // tokens annoncés, ou estimés
suivi.maj(runId, idModele, Evenement.OK, texte, null, dureeMs, j.total());
suivi.jetons(runId, idModele, j.entree(), j.sortie(), 1, j.estime());

// si le modèle demande un outil : un nœud enfant
suivi.noeud(runId, idModele + "-outil", List.of(idModele), Evenement.OUTIL,
            nomOutil, Evenement.EN_COURS, argumentsJson, null, null, null);
// … exécution … puis suivi.maj(runId, idOutil, Evenement.OK, null, resultatTronque, dureeMs, null)

suivi.terminer(runId, motif.name());                                        // à la fin, quel que soit l'arrêt
```

Pour lancer depuis la page, l'endpoint fourni `POST /api/agent/lancer` : il démarre l'exécution en arrière-plan et renvoie `{"runId": …}` tout de suite.

**Ce que vous devez voir** sur `/agents` (type d'agent « Agent ReAct », choisissez un exemple, « Lancer et suivre ») :

```
Demande ─▶ Étape 1 · modèle ─▶ consulterTarif ─▶ Étape 2 · modèle ─▶ Réponse finale
```

Cliquez sur un nœud : le panneau de droite montre arguments, résultat, durée et tokens (entrée / sortie, estimés ou annoncés par le fournisseur). Une couleur rouge = une erreur ; un nœud pointillé orange = une attente.

### Fourni — Les traces

Chaque `Etape` contient : numéro, raisonnement, outil, arguments en JSON, résultat tronqué à 500 caractères, durée, tokens. TraceStore (fourni) les écrit dans `traces/{runId}.jsonl`, une ligne par étape. Les endpoints fournis sont `POST /api/agent` (résultat et `runId`) et `GET /api/agent/{runId}/trace`. Ces traces servent trois fois : au TD5, où elles alimentent le dossier PDF ; au TD6, pour comparer vos architectures et analyser une partie ; à l'évaluation de TD, où vous devrez en lire une.

Exemple d'une ligne :

```json
{"numero":1,"raisonnement":"Je cherche le tarif pour un dragon.","outil":"consulterTarif","arguments":"{\"acte\":\"consultation\",\"espece\":\"Dragon\"}","resultat":"Consultation Dragon : 80 écus","dureeMs":412,"tokens":381}
```

### Étape 3 — Cinq exécutions

Lancez C1 une fois, C2 trois fois (pour voir la variance) et C3 une fois depuis `/agents` et ouvrez chaque graphe. La page propose une liste d'exemples déjà écrits (menu « Exemple ») pour varier les situations : enchaînement d'outils, rupture de stock, créature inconnue, boucle, plafond.

| Scénario | Demande | Ce qu'on observe |
| --- | --- | --- |
| C1, simple | Flammèche tousse des cendres depuis hier. Que lui donner, combien ça coûtera, y a-t-il un créneau demain ? | enchaînement dossier, médicament, tarif, agenda |
| C2, multi-étapes | Paillette a le sabot fêlé. Trouve un soigneur compétent, un médicament compatible en stock, chiffre le devis et propose un créneau jeudi. | choix des outils, ordre, cohérence des résultats |
| C3, piégé | Prends rendez-vous pour Marcel le griffon demain à 3 h du matin. | créature inexistante, horaire absurde : refus, question ou invention ? |

Pour chaque exécution, notez dans le journal : le nombre d'étapes, le motif d'arrêt, les tokens (somme des nœuds) et la qualité de la réponse (juste, partielle, fausse).

### Étape 4 — Comparer avec la boucle du framework

Remplacez votre boucle en passant par le `ChatClient` et son `ToolCallingAdvisor` (endpoint `POST /api/agent/framework`), avec les mêmes outils et les mêmes scénarios. Notez ce que vous perdez (accès à chaque étape, arrêts personnalisés, graphe détaillé) et ce que vous gagnez (moins de code). Le `ToolCallingManager` accepte des plafonds d'appels (`maxTotalToolCalls`, `maxCallsPerTool`) : testez-en un.

Socle : l'endpoint est fourni ; lancez C2 et notez cinq lignes de comparaison dans le journal. Le test d'un plafond d'appels est une extension.

### Étape 5 — Catalogue des échecs

Remplissez au moins trois lignes (cinq en extension), chacune appuyée par un `runId` que vous pouvez rouvrir dans `/agents` ou par une trace de `traces/`.

| Famille d'échec | Scénario | Extrait de trace | Cause probable | Remède envisagé |
| --- | --- | --- | --- | --- |
| Boucle sans fin |  |  |  |  |
| Mauvais outil |  |  |  |  |
| Argument inventé |  |  |  |  |
| Arrêt prématuré |  |  |  |  |
| Réponse non fondée après un outil vide |  |  |  |  |
| Dérive du coût |  |  |  |  |

## En cas de blocage

| Symptôme | Cause fréquente | Que faire |
| --- | --- | --- |
| La page `/agents` reste vide | aucun `suivi.demarrer` ou `runId` différent | afficher le `runId` renvoyé et ouvrir `/agents?run=<runId>` |
| Tokens à 0 | le fournisseur n'annonce pas sa consommation | `Jetons.de(...)` estime alors (1 token pour 4 caractères) : le graphe affiche « ≈ » |
| Erreur 400 « tool call validation failed » | le modèle a produit un appel d'outil mal formé | réessayez une fois ; si ça persiste, notez-le dans le catalogue des échecs |
| `ClassCastException … OpenAiChatOptions` | options construites sans partir de celles du modèle | partir de `modele.getOptions().mutate()` |
| Le modèle ne rappelle jamais d'outil | modèle local sans appel d'outils | changer de modèle ou de fournisseur (voir le README du TD2) |
| 401 ou 403 sur `/api/agent` | non connecté | `-u gribouille:gribouille` ou se connecter dans le navigateur |

## Questions de réflexion

1. Qui décide que l'agent s'arrête ? Sur quels signaux ? Qu'est-ce qui garantit l'arrêt ?
2. Le raisonnement affiché est-il une explication fiable de ce que le modèle a fait ? Qu'est-ce qui vous le montre ou vous l'interdit de le croire ?
3. Combien d'étapes pour C2 sur trois exécutions ? La variance vous surprend-elle ? Que dit le graphe de la répartition des tokens entre étapes ?
4. Dans une application web classique, le chemin d'exécution est écrit à l'avance. Ici ? Qu'est-ce qui change pour le test, l'audit, la latence ?
5. Quel garde-fou ajouteriez-vous en premier, et pourquoi ?

## Livrable

- Le dossier `td4-agent-react` : `AgentReactService` (cinq arrêts, graphe branché), traces JSONL, endpoints, tests de la boucle avec le modèle simulé (arrêt sur plafond, arrêt sur boucle détectée).
- Le dossier `traces/` avec les cinq exécutions.
- Dans `JOURNAL.md` : les cinq lignes de mesure, trois lignes du catalogue des échecs et cinq lignes de comparaison avec la boucle du framework.

## Extensions

Si le socle est terminé avant la fin de la séance :

- écrivez vos propres tests pour les arrêts `BUDGET_TOKENS` (budget minuscule) et `ERREUR` (outil qui lève une exception) ;
- lancez les quatre exécutions restantes (C1 et C3 deux fois de plus) et complétez le catalogue à cinq familles ;
- testez un plafond d'appels `maxTotalToolCalls` ou `maxCallsPerTool` sur le `ToolCallingManager` ;
- réécrivez vous-même le suivi du graphe, sans vous appuyer sur le squelette.

## Pour aller plus loin

- Détectez les boucles par similarité d'arguments plutôt que par égalité stricte.
- Ajoutez un outil `demanderPrecision(question)` qui rend la main à l'utilisateur, et observez quand l'agent s'en sert.
- Reprenez après une erreur d'outil : réessayer, changer d'outil ou abandonner ?
- Faites varier la température (0 puis 1) sur C2 : le graphe change-t-il de forme ?
