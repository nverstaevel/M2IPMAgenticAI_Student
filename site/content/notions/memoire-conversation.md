---
title: Mémoire de conversation
resume: Le modèle n'a pas de mémoire : l'application renvoie l'historique à chaque appel.
cm: CM1 — Un échange est une liste de messages ; La mémoire, c’est à l’application de la gérer
td: td2
---
Un LLM est **sans état** : « Et son propriétaire ? » n'a de sens que si l'historique est renvoyé. C'est le rôle de la mémoire :

- `MessageWindowChatMemory` garde les N derniers messages (fenêtre glissante, 10 au TD2) ;
- `MessageChatMemoryAdvisor` ajoute l'historique à chaque requête ;
- le **dépôt** (`ChatMemoryRepository`) décide où c'est stocké : en mémoire (perdu au redémarrage) ou en JDBC (profil `persistant`) ;
- l'**identifiant de conversation** est obligatoire, sinon toutes les conversations se mélangent.

Conséquence : plus la conversation est longue, plus chaque appel coûte cher (voir *Tokens*).
