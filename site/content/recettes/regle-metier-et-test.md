---
title: Ajouter une règle métier au service et la tester
resume: Écrire la règle dans le service, la signaler par une exception claire, la vérifier par un test.
td: td1
notions: couches-spring, spring-data-services, api-rest-validation
---
## Objectif
Faire respecter « deux consultations ne se chevauchent pas » **au bon endroit**, avec un message utile et un test qui le prouve.

## Les étapes
1. **Dans le service**, avant d'enregistrer : cherchez les consultations du soigneur qui chevauchent la nouvelle.
2. Deux intervalles `[a1, a2)` et `[b1, b2)` se chevauchent si `a1 < b2 && b1 < a2`.
3. Si oui, levez une **exception métier** dédiée avec un message qui nomme le soigneur et l'heure.
4. Dans le contrôleur (ou le `@RestControllerAdvice`), traduisez-la en **409**.
5. Écrivez le test (ci-dessous).

## Squelette du test
```java
@Test
void refuseUnCreneauQuiChevauche() {
    service.planifier(/* soigneur, créature, 10h–11h */);
    assertThatThrownBy(() -> service.planifier(/* même soigneur, 10h30–11h30 */))
        .isInstanceOf(ConflitDeCreneauException.class)
        .hasMessageContaining("10");
}
```

## Vérifier
- le **message** est lisible (il remontera dans le chat au TD2) ;
- un créneau qui commence **exactement** à la fin du précédent est accepté ;
- aucune écriture partielle après l'échec (transaction).

## Pièges
- Placer la règle dans le contrôleur : l'agent la contournera.
- Attraper l'exception dans le service : la transaction se valide malgré l'échec.
