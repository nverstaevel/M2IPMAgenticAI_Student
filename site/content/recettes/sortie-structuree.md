---
title: Obtenir un objet Java plutôt qu'un texte
resume: Un `record`, un client séparé, `.entity(...)`.
td: td2, td5
notions: sortie-structuree
---
## Squelette
```java
enum Urgence { FAIBLE, MOYENNE, ELEVEE }
record Triage(String espece, String symptome, Urgence urgence) {}

@Bean
ChatClient clientTriage(ChatClient.Builder builder) {   // sans mémoire, sans outils
    return builder.defaultSystem("Tu résumes la demande et estimes l'urgence.").build();
}

Triage t = clientTriage.prompt()
        .user("Ma licorne boite depuis ce matin et refuse de manger.")
        .call()
        .entity(Triage.class);
```

## Pièges
- Deux `ChatClient` en bean : précisez lequel injecter (`@Primary` ou `@Qualifier`).
- Réponse hors format : le modèle a mélangé texte et JSON ; essayez de simplifier le `record`, ou retentez une fois.
- Sur certains fournisseurs, sortie structurée **et** outils dans le même appel se gênent : séparez les deux temps (voir TD5).
