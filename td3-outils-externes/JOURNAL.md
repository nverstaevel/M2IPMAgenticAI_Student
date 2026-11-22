# Journal de bord

Notez ici, à chaque TD : ce que vous avez fait, ce qui a cassé, ce que vous avez compris, ce que l'IA vous a suggéré et ce que vous en avez gardé ou rejeté.

## TD1
- 

## TD3
### Étape 0 — Tableau de conception
| Source | Question typique | Outil | Lecture / écriture | Ce qu'il renvoie | Si la source échoue | Qui a écrit le texte ? |
| --- | --- | --- | --- | --- | --- | --- |
| Wikipedia | « Que mange un phénix ? » | `chercherDansWikipedia` | | | | |
| Excel | « Combien coûte un vaccin pour un yéti ? » | | | | | |
| Base de médicaments | « Le baume ignifuge est-il sûr pour mon dragon ? » | | | | | |
| Agenda | « Avez-vous un créneau jeudi ? » | | | | | |

### Étapes 1 à 4
- 

### Essais avec le LLM (modèle utilisé : …)
| Question posée | Outil attendu | Outil appelé (arguments) | Réponse juste ? | Si non : cause et correction |
| --- | --- | --- | --- | --- |
| Parle-moi du phénix | `chercherDansWikipedia` | | | |
| Combien coûte une consultation pour un yéti ? | `consulterTarif` | | | |
| Reste-t-il de la poudre de lune ? | `stockPharmacie` | | | |
| Le baume ignifuge est-il sûr pour un dragon ? | `chercherMedicament` | | | |
| Donne-moi une dose de baume pour un vampire. | `chercherMedicament` (aucun résultat) | | | |
| Avez-vous un créneau le 3 décembre 2026 ? | `creneauxLibres` | | | |
| Réserve le 4 décembre 2026 à 11 h Flammèche, rhume. | `proposerEvenementAgenda` | | | |

### Étape 5 — Synthèse de l'expérience
| Stratégie | tailleMax | Jetons moyens | Latence moyenne (ms) | Coût total | Exactes (sur 10) |
| --- | --- | --- | --- | --- | --- |
| A | 5000 | | | | |
| B | 5000 | | | | |
| A | 20000 | | | | |
| B | 20000 | | | | |
| A | 60000 | | | | |
| B | 60000 | | | | |

### Cinq conclusions chiffrées
1. 
