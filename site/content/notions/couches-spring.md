---
title: Les quatre couches d'une application Spring Boot
resume: Contrôleur, service, dépôt, domaine : chacun son rôle, un seul sens de dépendance.
cm: CM1 — Rappel Spring Boot : Quatre couches, un seul sens de dépendance
td: td1
---
## L'idée
Une application Spring Boot se range en quatre couches. Chacune a **une** responsabilité et ne connaît que la couche située juste en dessous.

## Comprendre
{{fig:couches}}

| Couche | Elle sait faire | Elle ne doit jamais |
|---|---|---|
| `controller` | lire une requête HTTP, appeler un service, choisir le code de réponse | contenir une règle métier |
| `service` | appliquer les règles, enchaîner les dépôts, gérer la transaction | connaître HTTP (`HttpServletRequest`, codes d'état) |
| `repository` | lire et écrire en base | décider d'une règle |
| `domain` | décrire les données (entités) | appeler un service |

**Pourquoi tant de rigueur ?** Parce qu'au TD2 un *deuxième client* arrive : le modèle de langage. Ses outils appellent les **services**. Si la règle « pas de chevauchement de consultations » est dans le service, elle protège l'API REST *et* l'agent. Si elle était dans le contrôleur, l'agent la contournerait.

## Dans la clinique
`planifierConsultation(...)` vit dans `CliniqueService`. Elle vérifie que le soigneur soigne l'espèce, que le créneau est libre, puis enregistre. Le contrôleur ne fait que traduire l'exception `ConflitDeCreneauException` en code 409.

## À retenir
- Une règle métier = **un seul endroit** : le service.
- Les outils du modèle appellent le service, jamais un dépôt ni un contrôleur.
- Si vous écrivez `if` sur une règle métier dans un contrôleur, remontez-la dans le service.

## Erreurs fréquentes
- Injecter un `Repository` directement dans un contrôleur (ou dans un outil du modèle).
- Mettre `@Transactional` sur le contrôleur plutôt que sur le service.

## Auto-test
<details class="qa" markdown="1"><summary>Où placer « un soigneur ne soigne pas toutes les espèces » ?</summary>

Dans le **service** (`planifierConsultation`), avant l'enregistrement : ainsi l'API REST et l'outil du modèle sont protégés par la même règle.

</details>

<details class="qa" markdown="1"><summary>Un outil du modèle peut-il appeler `CreatureRepository` ?</summary>

Non : il passe par le service, sinon il contourne les règles métier et les transactions.

</details>
