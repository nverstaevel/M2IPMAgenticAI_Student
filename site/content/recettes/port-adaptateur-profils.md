---
title: Isoler une dépendance externe (port + deux adaptateurs)
resume: Une interface, une version réelle, une version simulée, choisies par une propriété de configuration.
td: td3
notions: port-adaptateur, tester-sans-modele, profils-secrets
---
## Le schéma
{{fig:port_adaptateur}}

## Quand l'utiliser
Dès qu'un outil dépend de quelque chose qui demande un réseau, une clé ou un compte : sans isolation, vous ne pouvez ni le tester, ni l'évaluer de façon reproductible.

## Squelette (exemple hors clinique : une météo)
```java
// 1. Le port : ce dont le domaine a besoin, rien de plus
public interface MeteoPort {
    double temperature(String ville);            // lecture seule
}

// 2. Deux adaptateurs
class MeteoHttp implements MeteoPort {            // le vrai : appelle un service sur le réseau
    public double temperature(String ville) { /* RestClient … */ return 0; }
}
class MeteoSimulee implements MeteoPort {         // le faux : des valeurs fixes, en mémoire
    public double temperature(String ville) { return 18.0; }
}

// 3. Un seul est créé, selon une propriété (défaut : le simulé)
@Configuration
class MeteoConfig {
    @Bean @ConditionalOnProperty(name = "meteo.mode", havingValue = "simule", matchIfMissing = true)
    MeteoPort simulee() { return new MeteoSimulee(); }

    @Bean @ConditionalOnProperty(name = "meteo.mode", havingValue = "http")
    MeteoPort http() { return new MeteoHttp(); }
}
```

L'outil (`@Tool`) ne connaît que `MeteoPort`. Pour passer au réel : `meteo.mode=http` dans `application-local.yml`, sans toucher au code de l'outil.

## À tester
- avec le simulé, l'outil renvoie toujours la même valeur : le test est **déterministe** et ne touche pas le réseau ;
- un test qui démarre l'application sans aucune propriété trouve bien **un seul** bean `MeteoPort`.

## Pièges
- Créer les deux adaptateurs sans condition : deux beans pour un port, erreur au démarrage.
- Mettre dans le port une méthode qui n'a de sens que pour un adaptateur (un en-tête HTTP, un identifiant Google) : le port doit rester **neutre**.
- Un simulé trop gentil : il ne reproduit jamais l'erreur, donc votre outil n'a jamais à la gérer. Ajoutez au moins un cas d'échec.

## Dans le TD
TD3, étape 4 : `AgendaPort` avec `GoogleAgendaAdapter` (fourni) et `AgendaSimule` (à écrire), choisis par `agenda.mode`.
