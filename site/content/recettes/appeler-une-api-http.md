---
title: Appeler une API HTTP depuis un outil
resume: RestClient, en-tête User-Agent, gestion des erreurs, résultat court.
td: td3
notions: appel-outils, texte-tiers-injection, port-adaptateur
---
## Squelette (exemple hors clinique)
```java
@Component
class ClientRecettes {

    private final RestClient client = RestClient.builder()
            .baseUrl("https://exemple.org/api")
            .defaultHeader(HttpHeaders.USER_AGENT, "MonAppli/1.0 (cours M2 MIAGE; contact@exemple.org)")
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();

    Optional<String> resume(String titre) {
        try {
            String texte = client.get().uri("/resume/{t}", titre)
                                 .retrieve().body(String.class);
            return Optional.ofNullable(texte);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();                  // page absente : un cas normal, pas une exception
        }
    }
}
```

## À faire pour un outil
- **tronquer** la réponse (1 500 caractères au TD3) ;
- renvoyer un **message clair** quand il n'y a rien (« Aucune page ne porte ce titre ») ;
- traiter les pages d'**homonymie** ;
- considérer le texte reçu comme **non fiable** (voir *Texte écrit par un tiers*).

## Tester sans réseau
Construisez le client avec un `RestClient` préconfiguré (serveur simulé) dans le constructeur de test, et lisez des réponses JSON d'exemple dans `src/test/resources`.

## Pièges
- Wikimedia refuse les requêtes **sans User-Agent** identifiable (403).
- Un titre avec espaces ou accents : laissez `uri("/…/{t}", titre)` faire l'encodage, ne concaténez pas l'URL à la main.
