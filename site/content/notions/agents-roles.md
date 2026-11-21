---
title: Agents à rôles : acteur, critique, superviseur
resume: Séparer ceux qui proposent, ceux qui vérifient et ceux qui coordonnent.
cm: CM2 — Un modèle en critique un autre ; Acteur et critique : un tour de boucle ; Un superviseur qui délègue
td: td5, td6
---
- **Acteur / critique** : l'acteur (vétérinaire) propose, le critique (gardien du bestiaire) vérifie avec **d'autres sources** et rend un verdict `APPROUVE`, `REJETE`, `DOUTE`. Un rejet renvoie au tour suivant, avec les objections, dans la limite de `max` tours.
- **Superviseur / travailleurs** : un planificateur découpe, des travailleurs exécutent, une synthèse assemble.
- **Principe de moindre privilège** : chaque rôle n'a que les outils dont il a besoin (le critique n'a aucun outil d'écriture).

À mesurer, pas à supposer : rejets corrects, faux rejets, faux accords, tours moyens, tokens, comparés à un **workflow classique** (le même enchaînement écrit en Java, sans modèle pour décider).
