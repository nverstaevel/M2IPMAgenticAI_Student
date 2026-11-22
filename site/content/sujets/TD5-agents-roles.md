# TD5 — Agents à rôles et dossier PDF

## Objectifs de la séance

La clinique engage une équipe : un vétérinaire propose un traitement, un gardien du bestiaire le confronte à la littérature fantastique via Wikipedia (pas d'argent pour soigner un loup-garou, pas de soupe à l'ail pour un vampire), et un superviseur sait déléguer. À partir de cette séance, vous ne réécrivez plus la boucle : vous **développez des agents et des outils**, vous **spécifiez l'architecture dans un fichier Markdown**, et vous **analysez** ce que l'orchestration produit.

À la fin de la séance, vous saurez :

1. **développer un agent** : un rôle, un prompt, ses outils et une sortie structurée, exposé comme bean `Agent` ;
2. **spécifier une architecture** dans un fichier `.md` sous forme de graphe en flèches (a -> b), avec les outils autorisés de chaque agent et ses boucles, que l'outil lit, dessine et exécute : boucle acteur-critique, puis superviseur et travailleurs ;
3. **lire le graphe d'une architecture multi-agents** : tours, retours, tokens par agent, comparaison entre le graphe prévu et le graphe réel ;
4. **mesurer** un duo (rejets corrects, faux rejets, faux accords, tours moyens) et le **comparer** à un workflow classique ;
5. **tenir l'humain dans la boucle** : un dossier PDF et une validation avant toute application.

**Vous avez réussi si** `mvn test` est vert, vous avez rejoué le cas 1 (profil pressé) et vu le graphe se refermer sur un rejet, un deuxième tour puis une approbation, et si vous pouvez expliquer chaque flèche du graphe.

Ce TD est surtout un TD d'**architecture** : vous décidez qui fait quoi, avec quels outils, dans quel ordre, et vous mesurez si ça marche. Le code Java des agents (clients, sortie structurée, PDF) est **déjà écrit et testé** ; le cœur du travail est dans les fichiers Markdown qui décrivent les graphes, les prompts, et dans l'analyse des résultats avec **votre LLM**.

| 🧰 Fourni, prêt à l'emploi (ne pas modifier) | 🧠 À vous |
| --- | --- |
| le moteur de scénarios (`Orchestrateur`, `ScenarioLoader`, `Agent`, `Entree`, `Resultat`…), `VeterinaireAgent` et `GardienDuBestiaire` (avec leurs prompts), les beans `veterinaire`, `gardien`, `travailleur` et `synthese`, `DuoService`, `DossierPdfService` complet, les contrats `Diagnostic`, `Critique`, `Verdict`, `TraiterConsultationWorkflow`, la page `/agents`, `SCENARIOS.md`, `scenarios/_modele.md`, `cas-bestiaire.csv` | le **planificateur** (1 bean, son prompt est votre conception), **deux fichiers `.md`** (le duo, le superviseur), les **prédictions** et la **mesure** du duo, **une amélioration mesurée** d'un prompt, la comparaison au workflow classique |

Chaque étape suit la même forme : **Objectif**, **Pas à pas**, **Point de contrôle**.

## Avant la séance

- [ ] Partez du dossier de départ `td5-agents-roles` (il contient votre TD4 corrigé plus le moteur de scénarios et les agents du duo) ou, pour garder votre propre TD4, copiez `td4-agent-react` en `td5-agents-roles` et ajoutez-y le code fourni. Vos outils et votre boucle ReAct du TD4 sont conservés ; vos traces sont la matière première du dossier.
- [ ] Lisez `SCENARIOS.md` (5 minutes) : la syntaxe des flèches (enchaînement, fan-out, jonction, boucle avec max, délégation), la section Agents avec les outils autorisés, l'interface `Agent`.
- [ ] Vérifiez `GET /api/diagnostic/modele`, puis ouvrez `/agents` : le type d'agent « Scénario (fichier .md) » liste les scénarios fournis.
- [ ] Relisez dans le support de CM2 : *Workflow ou agent ?*, *Acteur et critique*, *Orchestrateur et travailleurs*.

## Ressources

| Ressource | Pour quoi faire | Quand |
| --- | --- | --- |
| `SCENARIOS.md` et `scenarios/_modele.md` | le format d'un scénario et d'un `Agent` | étapes 0, 3 et 6 |
| Support de CM2, « Acteur et critique », « Orchestrateur et travailleurs » | les schémas et les risques de chaque architecture | avant la séance |
| [Building effective agents](https://www.anthropic.com/engineering/building-effective-agents) (Anthropic) | workflow ou agent, patrons « évaluateur-optimiseur » et « orchestrateur-travailleurs » | avant ou après l'étape 3 |
| [Spring AI : Structured Output](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html) | `.entity(Diagnostic.class)` | étapes 1 et 2 |
| [Spring AI : ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html) | un client par agent, outils par agent | étapes 1 et 2 |
| [Apache PDFBox](https://pdfbox.apache.org/) | générer le dossier PDF | étape 5 |
| [Reflexion](https://arxiv.org/abs/2303.11366) (Shinn et al., 2023) | pour aller plus loin : critique et reprise | facultatif |

## Les cinq cas du bestiaire

L'acteur dispose de deux profils : `PRUDENT` (il vérifie avant de proposer) et `PRESSE` (il propose le premier remède qui lui vient). Utilisez `PRESSE` pour provoquer les erreurs.

| Cas | Créature et symptôme | Traitement du vétérinaire pressé | Verdict attendu |
| --- | --- | --- | --- |
| 1 | Lunatique (loup-garou), insomnie les soirs de pleine lune | compléments à base d'argent | REJETE : l'argent est réputé nocif aux loups-garous |
| 2 | Monsieur Crépuscule (vampire), grande fatigue | soupe à l'ail et promenade au soleil | REJETE : ail et soleil sont réputés nocifs aux vampires |
| 3 | Flammèche (dragon), toux de cendres | sirop apaisant et repos | APPROUVE |
| 4 | Rebelote (phénix), rhume | traitement ordinaire, sans particularité | APPROUVE (test du faux rejet) |
| 5 | Grognon (yéti), allergie estivale | bain chaud et sauna | DOUTE : les sources sont maigres |

Ces verdicts suivent le folklore courant, pas une vérité scientifique. Les pages Wikipedia peuvent nuancer : le critique doit citer ce qu'il a lu, et c'est sa citation que vous jugerez.

## Parcours guidé

Chaque étape se termine par un **point de contrôle**.

| Étape | Durée | Vous faites | Point de contrôle |
| --- | --- | --- | --- |
| 0. Prise en main du moteur | 10 min | lancer `react-clinique`, aperçu puis graphe réel | vous distinguez graphe prévu et graphe réel |
| 1. L'acteur | 15 min | lire `VeterinaireAgent`, lancer le cas 3 | le diagnostic du cas 3 apparaît dans un nœud |
| 2. Le critique | 20 min | lire `GardienDuBestiaire`, lancer le cas 1 pressé | le cas 1 est REJETE, avec une source citée |
| 3. Le duo en .md | 25 min | écrire `duo-veterinaire-gardien.md`, **prédire** le graphe | rejet, deuxième tour, approbation sur le graphe |
| 4. Mesurer et améliorer | 35 min | cinq cas, un tableau, **une amélioration mesurée** | tableau avant / après dans `JOURNAL.md` |
| 5. Dossier PDF | 15 min | lire le dossier, valider en tant que PERSONNEL | un PDF par cas, bouton Valider réservé au PERSONNEL |
| 6. Le superviseur en .md | 30 min | le planificateur, le `.md`, prédire le plan | un plan, trois travailleurs verts, une synthèse |
| 7. Workflow classique | 15 min | lancer `TraiterConsultationWorkflow`, remplir le tableau | tableau rempli sur cinq exécutions |

Le socle (étapes 0 à 7) représente environ 2 h 45. Si le temps manque, les étapes 0 à 5 sont indispensables ; terminez 6 et 7 chez vous. Le reste de la séance est pour les extensions.

### Étape 0 — Prise en main du moteur

**Objectif.** Savoir lire ce que le moteur dessine. Notions : *Scénarios en Markdown*, *Lire une trace*.

Le moteur lit des fichiers `.md` dans `scenarios/` (relus à chaque actualisation de la page) et dans les ressources.

**Pas à pas.**

1. Sur `/agents`, choisissez le type « Scénario (fichier .md) », puis le scénario `react-clinique` : il appelle votre agent ReAct du TD4 par son nom de bean (`react`).
2. Cliquez **Aperçu du graphe** : le graphe *prévu* s'affiche en pointillés, sans rien exécuter.
3. Cliquez **Lancer et suivre** : le graphe *réel* se dessine. En fin d'exécution, la page indique les nœuds prévus non atteints et les nœuds non prévus.

Retenez la règle du moteur : **l'agent ne s'occupe de rien d'autre que de son travail**. Le moteur crée le nœud de chaque appel, mesure sa durée, retente une fois un appel refusé par le fournisseur, et fait remonter les tokens et les outils.

**Point de contrôle.** Vous savez expliquer, pour un nœud en pointillé et un nœud plein, ce qui était prévu et ce qui s'est passé.

### Les contrats (fournis)

`Diagnostic` (créature, diagnostic, traitements, justification), `Critique` (verdict, objections, sources consultées) et l'enum `Verdict` : `APPROUVE`, `REJETE`, `DOUTE`. Les deux agents répondent par sortie structurée.

### Étape 1 — L'acteur

**Objectif.** Comprendre ce qui fait d'un agent un « rôle » : un prompt, des outils, une sortie structurée. Notions : *Agents à rôles*, *Sortie structurée*.

**Pas à pas.**

1. 🧰 Lisez `VeterinaireAgent` : le prompt `BASE` et les trois profils (`PRUDENT`, `PRESSE`, `OBSTINE`), ses outils (dossier, pharmacie, tarifs), et la sortie `Diagnostic`. À partir du deuxième tour, il reçoit les **objections** du critique.
2. 🧰 Lisez le bean `veterinaire` dans `scenario/AgentsExemples` : il traduit l'`Entree` du moteur en appel à l'agent. Le moteur lui donne ses tokens et ses outils.
3. 🧠 Dans le journal : en quoi les profils `PRUDENT` et `PRESSE` diffèrent-ils dans le prompt ? Qu'attendez-vous comme traitement du cas 1 avec chacun ?
4. Lancez le cas 3 (dragon, toux de cendres) avec le profil `PRUDENT` depuis `/agents`, avec votre LLM.

**Point de contrôle.** Le diagnostic du cas 3 apparaît dans un nœud ; vous avez comparé votre prédiction au traitement obtenu.

### Étape 2 — Le critique

**Objectif.** Comprendre le principe du moindre privilège : un critique qui ne peut pas agir. Notions : *Agents à rôles*, *Moindre privilège*. Recette : *Donner ses outils à un rôle*.

**Pas à pas.**

1. 🧰 Lisez `GardienDuBestiaire` : un seul outil, `chercherDansWikipedia`, **aucun outil d'écriture**. Il vérifie chaque traitement contre les pages de la créature et de l'espèce, cite ses sources et rend un verdict.
2. 🧰 Lisez le bean `gardien` : il rend un **avis** (`Resultat.avis(decision, objections)`), que le moteur utilise pour décider de reboucler.
3. 🧠 Dans le journal : pourquoi le critique n'a-t-il aucun outil d'écriture ? Que pourrait-il arriver sinon ?
4. Lancez le cas 1 (Lunatique, loup-garou) avec le profil `PRESSE`, avec votre LLM.

**Point de contrôle.** Le cas 1 pressé est REJETE avec une source Wikipédia citée dans le détail du nœud.

### Étape 3 — Le duo, spécifié en Markdown

**Objectif.** Décrire une architecture sans la coder : le graphe, les outils autorisés, la boucle. Notion : *Scénarios en Markdown*. Recettes : *Écrire un scénario*, *Duo acteur-critique*.

Le fichier a trois sections : Entrées (les champs de la page), Agents (les beans Java et leurs outils autorisés) et Graphe (les flèches). Le comportement des agents reste dans le code Java ; le `.md` ne fait que les câbler.

**Pas à pas.**

1. 🧠 **Prédisez avant d'écrire** : pour le cas 1 pressé, combien de tours, quelles flèches « retour », quel statut final ? Notez-le.
2. 🧰 Créez `src/main/resources/scenarios/duo-veterinaire-gardien.md` avec ce contenu (à lire ligne par ligne) :

```markdown
# Duo vétérinaire / gardien du bestiaire
Le vétérinaire propose, le gardien critique. S'il rejette, le vétérinaire reprend avec
les objections (2 reprises au plus) ; s'il doute, la décision part vers un humain.

## Entrées
- cas (Cas du bestiaire) : 1 = Cas 1 | 2 = Cas 2 | 3 = Cas 3 | 4 = Cas 4 | 5 = Cas 5
- profil (Profil du vétérinaire) : PRUDENT = Prudent | PRESSE = Pressé

## Agents
- veterinaire : outils dossier, pharmacie
- gardien : outils bestiaire

## Graphe
entrée -> veterinaire -> gardien
gardien -> veterinaire : REJETE (max 2)
gardien -> humain : APPROUVE, DOUTE, REJETE
humain -> fin
```

3. Sur `/agents`, choisissez ce scénario : les champs apparaissent, les erreurs du fichier aussi (agent ou outil inconnu, boucle sans étiquette). Cliquez **Aperçu du graphe**, puis lancez le cas 1 avec le profil pressé.
4. 🧠 Comparez avec votre prédiction. Puis **changez une ligne** du graphe (par exemple `max 1`, ou retirez `REJETE` de la sortie vers l'humain) et dites à l'avance ce qui change.

**Ce que vous devez voir :**

```
Entrée ─▶ veterinaire · tour 1 ─▶ gardien · tour 1 (rouge : REJETE)
                  ▲                      │
                  └── ↻ objections ────┘
          veterinaire · tour 2 ─▶ gardien · tour 2 (APPROUVE) ─▶ Validation humaine ─▶ Résultat : APPROUVEE
```

Une flèche pointillée orange étiquetée « ↻ » est un **retour** : les objections du critique reviennent vers l'acteur. Le moteur applique la boucle suivante, que vous n'avez pas à écrire :

```java
List<String> objections = List.of();
for (int tour = 1; tour <= MAX_TOURS; tour++) {
    Resultat p = producteur.executer(entree(tour, objections));
    Resultat c = critique.executer(entree(tour, p));
    if (c.decision() == APPROUVE) return approuvee();
    if (c.decision() == DOUTE)    return versHumain();
    objections = c.remarques();
}
return nonConvergente();
```

Cliquez sur un nœud du critique : *Détail* montre ses objections et ses sources ; *Tokens*, la part de chaque appel au modèle.

**Point de contrôle.** Rejet, deuxième tour, approbation sur le graphe ; votre prédiction et votre modification du graphe sont notées avec leur écart.

### Étape 4 — Mesurer et améliorer

**Objectif.** Évaluer le duo sur cinq cas, puis **améliorer une chose et mesurer l'effet**. Notion : *Évaluer un agent*. Recette : *Mesurer les tokens*.

**Pas à pas.**

1. Lancez les cinq cas, une fois chacun, profil pressé, avec votre LLM. Au rapport : rejets corrects, faux rejets (cas 3 et 4), faux accords (cas 1 et 2), nombre moyen de tours, tokens moyens (somme des nœuds).
2. 🧠 Choisissez **un** défaut observé (un faux rejet, un faux accord, trop de tokens, un critique qui n'a pas consulté Wikipédia).
3. 🧠 Changez **une seule** chose : une phrase de `GardienDuBestiaire.SYSTEME`, ou le profil de l'acteur. Relancez les cinq cas. (C'est la seule modification de code de cette étape.)
4. Remplissez le tableau avant / après :

| Mesure | Avant | Après |
| --- | --- | --- |
| Rejets corrects (sur 2) | | |
| Faux rejets (sur 2) | | |
| Faux accords (sur 2) | | |
| Tours moyens | | |
| Tokens moyens | | |
| Ce que j'ai changé | | |

**Point de contrôle.** Le tableau est rempli ; une phrase dit si l'amélioration est réelle ou si elle peut être due au hasard (une exécution par cas ne prouve pas grand-chose).

### Étape 5 — Le dossier PDF

**Objectif.** Tenir l'humain dans la boucle : rien n'est appliqué sans validation. Notion : *« Proposer, jamais écrire »*.

`DossierPdfService.generer(runId)` (fourni) produit un PDF à partir du `DossierRun` que renvoie `DuoService.traiter(cas, profil)` :

1. **En-tête** : clinique, date, patient (créature, propriétaire fictif), motif.
2. **Un bloc par tour** : la proposition du vétérinaire, le verdict du gardien, ses objections, les sources consultées avec leurs liens.
3. **Décision** : état `EN_ATTENTE_DE_VALIDATION`, puis `VALIDE` ou `REFUSE`, avec le nom de l'agent humain et l'horodatage.
4. **Annexe technique** : durée, tokens, modèle utilisé, profil de l'acteur.

**Pas à pas.**

1. 🧰 Lancez un cas, puis ouvrez `GET /api/dossiers/{runId}.pdf`. Lisez le PDF.
2. 🧠 Dans le journal : quelles informations un membre du personnel doit voir pour décider ? Manque-t-il quelque chose dans ce dossier ?
3. Connectez-vous en `visiteur` puis en `gribouille` : qui peut valider ? Pourquoi aucun traitement n'est-il appliqué tant que le dossier n'est pas validé ?

**Point de contrôle.** Un PDF par cas ; le bouton Valider n'est utilisable que par le rôle PERSONNEL.

### Étape 6 — Le superviseur, spécifié en Markdown

**Objectif.** Concevoir une architecture « orchestrateur et travailleurs ». Notions : *Agents à rôles*, *Scénarios en Markdown*. Recettes : *Écrire un scénario*, *Donner ses outils à un rôle*.

Un superviseur reçoit une demande (« Flammèche a de la fièvre : traitement, devis, rendez-vous »), produit un plan de sous-tâches, puis les confie à trois travailleurs qui n'ont chacun que leurs outils :

| Travailleur | Outils |
| --- | --- |
| `DOSSIER` | outils internes du TD2 |
| `PHARMACIE` | Excel et base de médicaments |
| `AGENDA` | créneaux et proposition d'événement |

**Pas à pas.**

1. 🧰 Les beans `travailleur` (un seul bean générique, qui n'utilise que les outils que le `.md` lui autorise : `e.outilsAutorises()`) et `synthese` sont fournis dans `AgentsExemples` : lisez-les.
2. 🧠 Écrivez le bean `planificateur` (marqué `TODO TD5`, avec les indications dans le code). Son **prompt système** est votre conception : qui sont les trois travailleurs, ce que chacun sait faire, ce que le planificateur ne doit pas faire.
3. 🧠 **Prédisez** : pour « Flammèche a de la fièvre : traitement, devis, rendez-vous », quel plan attendez-vous (quels travailleurs, dans quel ordre) ?
4. 🧰 Créez `scenarios/superviseur-clinique.md`. Le `.md` crée trois nœuds à partir du même bean, avec des outils différents :

```markdown
# Superviseur et travailleurs
Un planificateur découpe la demande, chaque tâche va à un travailleur spécialisé, une synthèse assemble.

## Entrées
- demande (Demande) : "Flammèche a de la fièvre : traitement, devis et rendez-vous."

## Agents
- planificateur
- dossier = travailleur : outils dossier
- pharmacie = travailleur : outils pharmacie
- agenda = travailleur : outils agenda
- synthese

## Graphe
entrée -> planificateur
planificateur => dossier, pharmacie, agenda
dossier, pharmacie, agenda -> synthese -> fin
```

5. Lancez-le avec votre LLM et comparez le plan obtenu à votre prédiction.

Sur le graphe : le plan (bleu), un nœud vert par tâche confiée avec les outils appelés en dessous, la synthèse. Un travailleur qui échoue ne fait pas échouer tout le plan : la synthèse sait ce qui manque. Le superviseur ne peut pas lui-même écrire dans l'agenda. Chaque agent est limité à 15 appels d'outils par exécution.

**Point de contrôle.** Un plan, trois travailleurs verts, une synthèse ; l'écart avec votre prédiction est noté.

### Étape 7 — Le workflow classique

Lisez et lancez (fourni) `TraiterConsultationWorkflow` en Java ordinaire : trouver la créature, choisir un soigneur compétent, choisir un médicament dans une table de correspondance espèce-symptôme, chiffrer, proposer le premier créneau libre. Aucun modèle. Comparez-le au superviseur sur le scénario C2 du TD4, cinq exécutions chacun :

| Critère | Workflow classique | Superviseur d'agents |
| --- | --- | --- |
| Taux de réussite sur 5 exécutions |  |  |
| Latence moyenne |  |  |
| Tokens et coût estimé |  |  |
| Lignes de code |  |  |
| Effort pour ajouter une espèce ou un symptôme |  |  |
| Ce qu'il sait faire que l'autre ne sait pas |  |  |

## En cas de blocage

| Symptôme | Cause fréquente | Que faire |
| --- | --- | --- |
| Le scénario est marqué ⚠ dans la liste | agent ou outil inconnu, boucle sans étiquette, flèche mal écrite (la ligne est indiquée) | lire le message sous la liste : il nomme le champ fautif |
| « aucun agent nommé … » | le nom du `.md` ne correspond pas au nom du bean | `@Bean("veterinaire")` et `producteur: veterinaire` doivent coïncider |
| Le fichier `.md` ne change rien | mauvais dossier | le dossier est `scenarios/` à côté du `pom.xml` ; actualisez la liste |
| Erreur 400 « attempted to call tool … » (Groq, modèles `gpt-oss`) | le modèle confond sortie structurée et outil | activer `agents.sortie-en-deux-temps=true` (déjà dans `application-groq.properties`) |
| « Sortie refusée : traitement vide ou trop long » | le vétérinaire répond hors format | demandez des traitements courts dans le prompt ; l'appel est retenté une fois |
| Tokens à 0 sur un agent | client sans `ComptageJetons.advisor()` | l'ajouter à `defaultAdvisors(...)` |

## Questions de réflexion

1. Le critique est un LLM qui lit Wikipedia. Quelles erreurs peut-il faire ? Mesurez les faux rejets et les faux accords. Qui critique le critique ?
2. Les sources fantastiques divergent d'une œuvre à l'autre. Comment le verdict `DOUTE` aide-t-il ? Le résultat serait-il le même avec un autre modèle ?
3. Pourquoi limiter à trois tours ? Que signifie `NON_CONVERGENTE` ? Que montre le graphe quand le critique rejette trois fois ?
4. Qu'est-ce qui est dit dans le `.md`, qu'est-ce qui reste dans le code Java ? Que perdez-vous à spécifier l'architecture hors du code, et que gagnez-vous ?
5. Votre dossier PDF est-il une vraie trace d'audit ? Que manque-t-il : prompts complets, version du modèle, graine, texte exact des pages lues ? Quelles données personnelles pourrait-il contenir ?
6. En production, choisiriez-vous le workflow ou le superviseur pour ce cas ? Dans quel cas la réponse s'inverse-t-elle ?

## Livrable

- Le dossier `td5-agents-roles` : vos agents (acteur, critique, planificateur, trois travailleurs, synthèse), vos fichiers `scenarios/*.md`, le dossier PDF généré pour les cinq cas (fichiers dans `dossiers/`), la validation humaine, la comparaison avec le workflow classique (fourni).
- Des tests avec le modèle simulé : la boucle s'arrête sur APPROUVE, sur DOUTE et sur le plafond de tours.
- Dans `JOURNAL.md` : les mesures du duo, le tableau comparatif et cinq lignes de conclusion.

## Extensions

Si le socle est terminé avant la fin de la séance :

- lancez chaque cas trois fois (15 exécutions) pour mesurer la variance du duo ;
- ajoutez une espèce et un symptôme à la table du workflow et notez l'effort, pour remplir la ligne correspondante du tableau comparatif ;
- faites échouer un travailleur du superviseur (outil indisponible) et lisez ce que dit la synthèse ;
- écrivez vous-même les contrats `Diagnostic`, `Critique` et `Verdict` avant de lire ceux qui sont fournis.

## Pour aller plus loin

- Ajoutez un troisième rôle, un contrôleur de gestion qui critique le devis à partir des tarifs : une séquence suffit : `entrée -> veterinaire -> controleur -> fin`.
- Écrivez la boucle acteur-critique avec `(max 1)`, puis ajoutez un routeur (`Resultat.route`) qui oriente vers deux travailleurs : que devient le graphe quand le critique rejette ?
- Faites exécuter les travailleurs en parallèle quand ils sont indépendants.
- Joignez au PDF les prompts exacts de chaque tour, dans une annexe.
