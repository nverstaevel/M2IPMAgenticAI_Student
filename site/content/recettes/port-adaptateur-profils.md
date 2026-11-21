---
title: Isoler une dépendance externe (port + deux adaptateurs)
resume: Une interface, une version réelle, une version simulée, choisies par profil.
td: td3
notions: port-adaptateur, tester-sans-modele
---
## Squelette
```java
public interface AgendaPort {
    List<Creneau> evenementsDuJour(LocalDate jour);     // lecture
    Evenement creer(Evenement e);                       // écriture : jamais appelée par un outil
}

@Component @Profile("!simule")
class GoogleAgendaAdapter implements AgendaPort { /* fourni */ }

@Component @Profile("simule")
class AgendaSimule implements AgendaPort {
    private final List<Evenement> evenements = new ArrayList<>();   // une liste en mémoire suffit
    /* à vous */
}
```

## Créneaux libres : l'idée
Parcourez la journée de 9 h à 18 h par pas de 30 minutes ; un créneau est libre s'il ne **chevauche** aucun événement.
Deux intervalles `[a1, a2)` et `[b1, b2)` se chevauchent si `a1 < b2 && b1 < a2`.

## À tester
- agenda vide → 18 créneaux libres ; un événement de 10 h à 11 h retire deux créneaux ;
- le créneau qui commence **exactement** à la fin d'un événement reste libre.

## Pièges
- Mettre la même annotation de profil sur les deux adaptateurs : deux beans, une erreur de démarrage.
- Confondre les fuseaux horaires : travaillez en `LocalDateTime`.
