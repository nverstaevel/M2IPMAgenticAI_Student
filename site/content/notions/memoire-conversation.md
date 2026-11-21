---
title: Mémoire de conversation
resume: Le modèle n'a pas de mémoire : l'application renvoie l'historique à chaque appel.
cm: CM1 — Un échange est une liste de messages ; La mémoire, c’est à l’application de la gérer
td: td2
famille: modele
ordre: 4
---
## L'idée
Un modèle n'a **aucune mémoire** : c'est l'application qui renvoie l'historique à chaque appel.

## Comprendre
{{fig:memoire}}

- `MessageWindowChatMemory` garde les **N derniers messages** (fenêtre glissante, 10 au TD2).
- `MessageChatMemoryAdvisor` ajoute cet historique à chaque requête.
- Le **dépôt** (`ChatMemoryRepository`) décide où c'est stocké : en mémoire (perdu au redémarrage) ou en base (profil `persistant`).
- L'**identifiant de conversation** est obligatoire : sans lui, toutes les conversations se mélangent.

## Dans la clinique
« Parle-moi de Flammèche. » puis « Et son propriétaire ? » : sans mémoire, le modèle ne sait pas qui est « son ». Avec, l'historique contient la réponse précédente.

## À retenir
- Mémoire = historique renvoyé + fenêtre bornée + identifiant de conversation.
- Chaque message gardé est repayé à chaque appel.

## Erreurs fréquentes
- Oublier de passer l'identifiant de conversation à chaque appel.
- Fenêtre trop large : coût et latence en hausse, sans gain.

## Auto-test
Q: Après redémarrage, la conversation a disparu. Pourquoi ?
R: Le dépôt de mémoire est en mémoire vive. Avec le profil `persistant`, il est stocké en base (JDBC).
