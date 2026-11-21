---
title: Écrire un scénario d'agents en .md
resume: Entrées, Agents, Graphe : le fichier minimal et les erreurs courantes.
td: td5, td6
notions: scenarios-md, agents-roles, moindre-privilege
---
## Partir du modèle
Copiez `scenarios/_modele.md` et renommez-le. Relisez `SCENARIOS.md` pour la syntaxe complète.

## Un duo acteur / critique
```markdown
# Duo vétérinaire / gardien du bestiaire

## Entrées
- cas (Cas du bestiaire) : 1 = Cas 1 | 2 = Cas 2 | 3 = Cas 3
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

## Un superviseur et ses travailleurs
```markdown
## Agents
- planificateur
- dossier = travailleur : outils dossier
- pharmacie = travailleur : outils pharmacie
- synthese

## Graphe
entrée -> planificateur
planificateur => dossier, pharmacie
dossier, pharmacie -> synthese -> fin
```
`nom = bean : outils …` crée un nœud à partir d'un bean existant (ici trois travailleurs, un seul bean). `=>` distribue les tâches du plan ; `->` enchaîne.

## Méthode
1. **Aperçu du graphe** (pointillés) : sans rien exécuter, vérifiez les flèches.
2. **Lancer et suivre** : le graphe réel se dessine.
3. Comparez : nœuds prévus non atteints, nœuds non prévus.

## Erreurs fréquentes
| Message | Cause |
|---|---|
| ⚠ agent ou outil inconnu | nom différent du bean Java, ou faute dans le nom de l'outil |
| « aucun agent nommé … » | `@Bean("veterinaire")` ≠ nom dans le `.md` |
| flèche mal écrite | la ligne est indiquée sous la liste |
| le fichier ne change rien | pas dans `scenarios/` à côté du `pom.xml` |
