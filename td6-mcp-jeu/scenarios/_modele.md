# Titre du scénario (affiché dans la page /agents)

Quelques phrases libres : ce que ce scénario cherche à montrer ou à tester. Tout ce qui n'est pas dans une des trois sections
« Entrées », « Agents » et « Graphe » est la description du scénario. Les lignes qui commencent par « // » sont ignorées.

## Entrées
// Les champs de la page. « id : "texte par défaut" » : une zone de texte.
- demande : "Combien coûte une consultation pour un dragon ?"
// « id : a | b | c » : une liste, la première valeur est le défaut. « valeur = libellé » pour un libellé lisible.
- profil (Profil du vétérinaire) : PRUDENT = Prudent | PRESSE = Pressé

## Agents
// « nom » : l'agent Java (bean) du même nom, sans outil.
// « nom = bean » : le nom dans le graphe, puis le bean Java (deux nœuds peuvent partager un bean).
// « : outils a, b » : les seuls outils autorisés. Groupes : dossier, pharmacie, agenda, bestiaire ; ou un outil isolé (chercherDansWikipedia).
- agent1 = react : outils dossier

## Graphe
// Une ligne = un ou plusieurs arcs. « entrée » et « fin » existent toujours ; « humain » est une validation par le personnel.
//   a -> b -> c               une suite
//   a -> b, c                 deux suites (b et c)
//   a, b -> c                 une jonction : c attend a et b
//   a -> b : REJETE           b seulement si le résultat de a est « REJETE » (APPROUVE, REJETE, DOUTE, ou l'étiquette d'un routeur)
//   a -> b : REJETE (max 2)   si b est déjà passé : une boucle, au plus 2 reprises (3 par défaut, 10 au plus)
//   a => b, c                 a délègue : seuls les agents que son plan désigne s'exécutent
entrée -> agent1 -> fin
