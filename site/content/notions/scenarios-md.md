---
title: Décrire une architecture d'agents en Markdown
resume: Trois sections (Entrées, Agents, Graphe) et des flèches : le moteur lit, dessine et exécute.
cm: CM2 — Un graphe d’agents, un fichier .md
td: td5, td6
---
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

- Le **nom** d'un agent dans le `.md` doit correspondre au nom du bean Java (`@Bean("veterinaire")`).
- Une flèche étiquetée (`: REJETE`) dépend du verdict ; `(max 2)` borne une boucle.
- Le fichier se place dans `scenarios/` à côté du `pom.xml` ; actualisez la liste dans `/agents`.
- Un ⚠ dans la liste nomme le champ fautif : lisez le message.

Référence complète : `SCENARIOS.md` et `scenarios/_modele.md` dans votre dossier de TD.
