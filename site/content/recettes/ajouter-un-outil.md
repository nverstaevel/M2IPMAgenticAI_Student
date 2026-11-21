---
title: Ajouter un outil au modèle
resume: Une méthode annotée, une bonne description, un branchement sur le client.
td: td2, td3
notions: appel-outils, couches-spring, tester-sans-modele, chatclient-prompt
---
## Quand l'utiliser
Chaque fois que le modèle doit lire ou faire quelque chose que vous seul connaissez.

{{fig:tool_calling}}

## Ingrédients
- une classe `@Component` qui contient les outils ;
- un **service** existant à appeler (jamais un dépôt, jamais un contrôleur) ;
- un `.defaultTools(...)` sur le `ChatClient`.

## Squelette (exemple hors clinique)
```java
@Component
class OutilsBibliotheque {

    private final CatalogueService catalogue;

    OutilsBibliotheque(CatalogueService catalogue) { this.catalogue = catalogue; }

    @Tool(description = "Cherche des livres dont le titre contient le texte donné. "
            + "Renvoie au plus 5 résultats (id, titre, auteur). À utiliser avant toute réponse sur un livre.")
    List<LivreDto> chercherLivres(
            @ToolParam(description = "Partie du titre, ex. « Dune »") String titre) {
        return catalogue.chercher(titre);
    }
}
```

```java
// à la construction du ChatClient
builder.defaultTools(outilsBibliotheque).build();
```

## Les quatre règles d'une bonne description
1. dire **ce que l'outil fait** et **ce qu'il renvoie** ;
2. dire **quand l'utiliser** (et quand ne pas l'utiliser) ;
3. donner un **exemple de valeur** pour chaque paramètre ;
4. **renvoyer peu** : quelques lignes, pas une table.

## Vérifier
1. Logs en `DEBUG` (`logging.level.org.springframework.ai=DEBUG`) : voyez l'appel d'outil et ses arguments.
2. Test avec le modèle simulé : `outil:chercherLivres:{"titre":"Dune"}`.

## Pièges
- Outil jamais appelé : description trop vague, ou modèle sans appel d'outils.
- Outil appelé avec un argument inventé : ajoutez un exemple dans `@ToolParam`.
- Un outil qui **écrit** : à ne pas faire (voir la recette *Proposer, confirmer*).
