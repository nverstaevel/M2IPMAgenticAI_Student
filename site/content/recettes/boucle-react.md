---
title: Écrire une boucle d'agent à la main
resume: Appeler le ChatModel, exécuter les outils demandés, recommencer, et borner la boucle.
td: td4
famille: agents
ordre: 1
notions: boucle-react, arrets-agent, appel-outils, workflow-ou-agent
---
## Le schéma
{{fig:react}}

## Repère de code (à adapter à la version fournie)
```java
ToolCallingManager gestionnaire = ToolCallingManager.builder().build();
ToolCallback[] outils = ToolCallbacks.from(outilsClinique, outilsExternes);
ToolCallingChatOptions options = ToolCallingChatOptions.builder().toolCallbacks(outils).build();

Prompt prompt = new Prompt(messagesInitiaux, options);
ChatResponse reponse = chatModel.call(prompt);

while (reponse.hasToolCalls()) {
    ToolExecutionResult resultat = gestionnaire.executeToolCalls(prompt, reponse);
    prompt = new Prompt(resultat.conversationHistory(), options);
    reponse = chatModel.call(prompt);
}
```

## À ajouter autour (c'est le TD)
1. un **compteur d'étapes** et le motif `PLAFOND_ETAPES` ;
2. la **signature** de chaque appel (outil + arguments) comparée à la précédente : `BOUCLE_DETECTEE` ;
3. le **budget de tokens** cumulés (fourni, à lire) ;
4. les **erreurs** (`ERREUR`) ;
5. `Map.of("conversationId", runId)` passé en contexte d'outil.

## Dessinez d'abord
Sur papier : états (appel du modèle, exécution d'outils, fin), transitions, qui les déclenche. Si vous ne pouvez pas dessiner la boucle, vous ne pouvez pas l'écrire.

## Pièges
- `ClassCastException … OpenAiChatOptions` : partez des options du modèle (`modele.getOptions().mutate()`).
- Une boucle sans arrêt tourne jusqu'au budget : testez chaque arrêt avec le modèle simulé.
