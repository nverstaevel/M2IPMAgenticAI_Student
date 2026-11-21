---
title: Donner une mémoire à un ChatClient
resume: Fenêtre glissante, conseiller de mémoire, identifiant de conversation, dépôt JDBC.
td: td2
notions: memoire-conversation, chatclient-prompt, tokens-contexte
---
{{fig:memoire}}

## Ingrédients
- `MessageWindowChatMemory` (fenêtre de N messages) ;
- `MessageChatMemoryAdvisor` ;
- un `ChatMemoryRepository` (autoconfiguré : mémoire vive, ou JDBC avec le profil `persistant`).

## Squelette
```java
@Bean
ChatMemory chatMemory(ChatMemoryRepository repository) {
    return MessageWindowChatMemory.builder()
            .chatMemoryRepository(repository).maxMessages(10).build();
}

// à la construction du client
builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())

// à chaque appel : l'identifiant de conversation est obligatoire
client.prompt().user(message)
      .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
      .call().content();
```

## Vérifier
- « Parle-moi de Flammèche. » puis « Et son propriétaire ? » : le modèle sait de qui on parle.
- Après redémarrage avec le profil `persistant`, la conversation est toujours là.

## Pièges
- Oubli de l'identifiant : toutes les conversations se mélangent (ou une erreur).
- Fenêtre trop grande : chaque appel coûte plus cher (tokens d'entrée).
