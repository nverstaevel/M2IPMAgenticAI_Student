---
title: Répondre en flux (SSE)
resume: Afficher la réponse au fil de l'eau avec `.stream().content()`.
td: td2
famille: modele
ordre: 5
notions: chatclient-prompt, api-rest-validation, memoire-conversation
---
## Côté serveur
```java
@GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> flux(@RequestParam String conversationId, @RequestParam String message) {
    return client.prompt()
            .user(message)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
            .stream()
            .content();          // un fragment de texte par événement
}
```

## Côté page (JavaScript)
```javascript
const source = new EventSource(`/api/chat/stream?conversationId=${id}&message=${encodeURIComponent(texte)}`);
source.onmessage = e => zone.textContent += e.data;
source.onerror = () => source.close();     // sinon le navigateur se reconnecte tout seul
```

## Pièges
- `EventSource` n'envoie que des `GET` : le message passe en paramètre d'URL.
- Pensez à fermer la source à la fin, sinon la requête est rejouée.
- Un endpoint protégé exige d'être connecté dans le navigateur.
