Ce cours a été conçu avec l'aide d'une IA générative. Cette page dit lequel, pour quoi faire, et ce que cela a consommé.

## Qui a fait quoi

- **Nicolas Verstaevel** (Université Toulouse Capitole, IRIT) : le projet pédagogique, les objectifs, le plan des séances, les choix techniques et pédagogiques, les essais sur machine, la relecture et la validation de tout ce qui est publié.
- **Une IA générative** : la rédaction d'une grande partie des sujets, des notions, des recettes et des slides ; le code Java des squelettes, des corrigés, du jeu `jeu-bestiaire` et de ses tests ; le générateur du site ; les captures d'écran et les schémas.

Les contenus produits par l'IA ont été relus et essayés par l'enseignant, mais ils peuvent contenir des erreurs : si un énoncé, un indice ou un bout de code vous semble faux, **dites-le**, c'est une information utile pour le cours.

## Fournisseur et modèle

| | |
|---|---|
| Fournisseur | **Anthropic** |
| Modèle (phase de finalisation) | **Claude Sonnet 5.5** (identifiant d'API `claude-sonnet-5-5`) |
| Mode d'utilisation | application **Claude** (claude.ai), session agentique avec accès à un espace de travail (lecture et écriture de fichiers, exécution de commandes, recherche web), puis envoi du code sur GitHub |
| Phases antérieures | d'autres modèles de la famille Claude ont pu être utilisés lors des premières phases de conception ; la version exacte n'a pas été relevée |

## Consommation

Mesure disponible, **partielle** : elle porte sur la dernière phase de travail (depuis la dernière reprise de la conversation), qui comprend notamment la mise au point du jeu, de la page de pilotage, du kill switch, de la troisième architecture, de la page Outils et des cartes du code.

| Indicateur | Valeur |
|---|---:|
| Requêtes au modèle | 115 |
| Tokens d'entrée (hors cache) | 266 |
| Tokens écrits en cache | 440 083 |
| Tokens relus depuis le cache | 29 744 221 |
| Tokens de sortie (texte produit) | 107 460 |

Pour lire ce tableau :

- Un **token** est un morceau de mot (environ trois quarts d'un mot en français).
- Les tokens **relus depuis le cache** sont le contexte (consignes, fichiers, historique) que le modèle relit à chaque requête : ils sont nombreux, mais comptés à un tarif bien plus bas que les autres.
- Les tokens **de sortie** sont ce que le modèle a effectivement écrit : environ 107 000 sur cette phase, soit l'équivalent de quelques centaines de pages.
- La répartition estimée de l'usage sur cette phase : environ **56 %** pour les instructions de l'assistant relues à chaque tour, **37 %** pour les fichiers et les commandes, **7 %** pour la conversation elle-même.
- Le **total du cours** est plus élevé que ce relevé : les phases précédentes ne sont pas mesurées. Cette page sera complétée si un relevé complet devient disponible.

## Ce que cela veut dire pour vous

- Le cours enseigne des agents qui s'appuient sur des modèles : il est cohérent de dire comment il a lui-même été produit avec l'un d'eux.
- Vous pouvez utiliser une IA dans vos TD (c'est le sujet), mais **vous restez responsable de ce que vous rendez** : vérifiez, essayez, et sachez expliquer.
- Pour votre propre `JOURNAL.md`, indiquez de la même façon quand une réponse vient d'un modèle, lequel, et ce que vous avez vérifié.
