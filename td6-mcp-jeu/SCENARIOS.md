# Décrire une architecture dans un fichier .md

Un scénario = un fichier `.md` qui **dessine un graphe d'agents avec des flèches**. Le comportement des agents (consignes, prompts) reste en Java ;
le `.md` ne fait que les **câbler**, restreindre leurs **outils** et déclarer les **boucles**.
Les fichiers du dossier `scenarios/` (à côté du `pom.xml`) sont relus à chaque actualisation de `/agents` : pas de redémarrage.
Les scénarios fournis (`src/main/resources/scenarios/`) servent d'exemples ; un fichier de `scenarios/` de même nom les remplace.
Les fichiers dont le nom commence par `_` sont ignorés (`_modele.md` est un gabarit). Les lignes commençant par `//` sont des commentaires.

## Le format

```
# Titre du scénario
Description libre (plusieurs lignes possibles).

## Entrées                        (facultatif : champs du formulaire de /agents)
- cas (Cas du bestiaire) : 1 = Cas 1 | 2 = Cas 2       <- liste de choix
- demande (Demande) : "texte par défaut"                <- champ texte

## Agents                         (nœuds du graphe)
- veterinaire                      <- bean Java « veterinaire »
- dossier = travailleur : outils dossier, pharmacie
                                   <- nœud « dossier » = bean « travailleur », avec ces seuls outils

## Graphe
entrée -> veterinaire -> gardien
gardien -> veterinaire : REJETE (max 2)
gardien -> humain : APPROUVE, DOUTE, REJETE
humain -> fin
```

| Écriture | Sens |
|---|---|
| `a -> b -> c` | enchaînement |
| `a -> b, c` | fan-out : b et c démarrent après a |
| `a, b -> c` | jonction : c attend a et b |
| `a -> b : REJETE, DOUTE` | arc **étiqueté** : emprunté seulement si a rend l'une de ces étiquettes (une décision de critique : `APPROUVE`, `REJETE`, `DOUTE`, ou l'étiquette d'un routeur) |
| `b -> a : REJETE (max 2)` | **boucle** : arc de retour, étiqueté, au plus 2 reprises (3 par défaut, 10 au plus). Le corps de la boucle est ré-exécuté avec les remarques du critique ; une fois le budget épuisé, l'arc suivant qui convient prend le relais |
| `p => a, b, c` | délégation : seuls les travailleurs nommés dans le plan de `p` s'exécutent |
| `entrée`, `fin`, `humain` | nœuds réservés : début, fin, validation par le personnel |

`outils` accepte des groupes (`dossier`, `pharmacie`, `agenda`, `bestiaire`) ou des noms d'outils isolés. Un agent sans `outils` n'en a aucun.
Les erreurs (agent inconnu, outil inconnu, boucle sans étiquette, nœud inaccessible…) sont affichées avec leur ligne dans `/agents`.

## Architectures classiques

```
// ReAct : l'agent possède sa propre boucle
entrée -> agent -> fin

// Séquence
entrée -> redacteur -> relecteur -> fin

// Acteur / critique
entrée -> producteur -> critique
critique -> producteur : REJETE (max 3)
critique -> fin : APPROUVE, DOUTE, REJETE

// Routeur
entrée -> routeur
routeur -> technique : TECHNIQUE
routeur -> administratif : ADMIN
technique, administratif -> fin

// Superviseur
entrée -> planificateur
planificateur => dossier, pharmacie, agenda
dossier, pharmacie, agenda -> synthese -> fin
```

## Écrire un agent

Un agent est un bean Spring qui implémente `Agent` (une seule méthode), référencé **par son nom de bean** dans la section `## Agents` :

```java
@Bean("mon-producteur")
Agent monProducteur(ChatClient.Builder builder) {
    ChatClient client = builder.clone().defaultAdvisors(ComptageJetons.advisor()).build();
    return e -> Resultat.texte(client.prompt().user(e.texte())
            .toolCallbacks(e.outilsAutorises()).call().content());
}
```

- `Entree` : `texte()`, `donnee("id", "défaut")` (champs de la page), `retours()` (remarques du critique), `tour()`, `proposition()`, `outils()` / `outilsAutorises()` (**uniquement** les outils permis par le `.md`).
- `Resultat` : `Resultat.texte(..)`, `Resultat.avis(Decision.APPROUVE | REJETE | DOUTE, remarques)`, `Resultat.plan(taches)`, `Resultat.route(texte, "ETIQUETTE")` pour un routeur.
- Le même bean peut servir plusieurs nœuds (`a = travailleur`, `b = travailleur`) avec des outils différents.
- Le moteur crée le nœud de chaque appel, mesure sa durée et plafonne à 15 les appels d'outils par agent (`SuiviOutils.MAX_APPELS_OUTILS`) et à 40 les passages par exécution.

## Observer et analyser

1. `/agents` → « Scénario (fichier .md) » → choisir le scénario : description, champs, erreurs éventuelles.
2. **Aperçu du graphe** : boucles dépliées jusqu'au maximum, sans rien exécuter.
3. **Lancer et suivre** : graphe réel en direct ; la page signale les nœuds prévus non atteints et les nœuds non prévus ; comparez retours, tokens et durées entre architectures.

## Le groupe d'outils `jeu` (TD6)

Avec le profil `jeu`, `outils jeu` donne à un agent les neuf outils du jeu (`etat_clinique`, `lister_patients`, `fiche_espece`, `carnet_soins`, `consulter_pharmacie`, `examiner_patient`, `soigner`, `commander`, `fin_de_journee`) ; on peut aussi les nommer un par un pour restreindre un rôle. Exemples : `scenarios/jeu-gribouille-seul.md`, `scenarios/jeu-equipe.md`. Une exécution = une journée ; la partie complète se lance par `POST /api/agent/jeu/partie`.
