# Journal de bord

Notez ici, à chaque TD : ce que vous avez fait, ce qui a cassé, ce que vous avez compris, ce que l'IA vous a suggéré et ce que vous en avez gardé ou rejeté.

## TD1
### Étape 1 — Tableau de conception
| Relation | Cardinalité | Obligatoire ? | data.sql | Qui porte la clé ? |
| --- | --- | --- | --- | --- |
| Créature → Espèce | | | `espece_id` | |
| Créature → Propriétaire | | | | |
| Consultation → Créature | | | | |
| Consultation → Soigneur | | | | |
| Soigneur ↔ Espèce | | | `soigneur_espece` | |

### Étape 3 — Questions de conception
- Pourquoi la règle de chevauchement est-elle dans le service ?
- Que se passe-t-il sans verrou si deux requêtes arrivent en même temps ?

### Étape 4 — Prédictions de l'API
| Requête | Code attendu | Observé |
| --- | --- | --- |
| GET /api/proprietaires?nom=brume | | |
| GET /api/creatures/9999 | | |
| POST /api/consultations avec {} | | |
| POST sur un créneau libre | | |
| le même POST, une seconde fois | | |
| POST avec une date invalide | | |

### Étape 6 — Matrice de sécurité
| Requête | anonyme | visiteur | gribouille |
| --- | --- | --- | --- |
| GET /api/proprietaires | | | |
| POST /api/consultations | | | |

### Notes
- 
