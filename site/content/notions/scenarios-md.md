---
title: Décrire une architecture d'agents en Markdown
resume: Trois sections (Entrées, Agents, Graphe) et des flèches : le moteur lit, dessine et exécute.
cm: CM2 — Un graphe d’agents, un fichier .md
td: td5, td6
---
## L'idée
Une architecture d'agents se **décrit** dans un fichier Markdown de trois sections ; le moteur le lit, dessine le graphe et l'exécute.

## Comprendre

{{fig:md_graphe}}

```markdown
# Titre du scénario

## Entrées
- cas (Cas du bestiaire) : 1 = Cas 1 | 2 = Cas 2
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

| Élément | Sens |
|---|---|
| `a -> b` | b s'exécute après a |
| `a -> b : REJETE` | seulement si le verdict de a est REJETE |
| `(max 2)` | borne une boucle |
| `a => b, c` | le plan de a est distribué à b et c |

- Le **nom** d'un agent dans le `.md` doit correspondre au nom du bean Java (`@Bean("veterinaire")`).
- Un ⚠ dans la liste nomme le champ fautif : lisez le message.
- Fichier à placer dans `scenarios/` à côté du `pom.xml`.

## À retenir
- On change d'architecture en **éditant un fichier**, sans recompiler.
- *Aperçu* (pointillés) avant *Lancer* (graphe réel).

## Erreurs fréquentes
- Un nom d'agent dans le graphe qui ne correspond pas au nom du bean : le scénario est marqué ⚠.
- Une boucle sans étiquette (`REJETE (max 2)`) : le moteur la refuse.
- Écrire le comportement des agents dans le `.md` : il ne câble que le graphe et les outils autorisés ; le comportement reste en Java.

## Auto-test
Q: Votre scénario affiche « agent inconnu ». Où regarder ?
R: Le nom dans la section Agents : il doit coïncider avec le nom du bean Java.
