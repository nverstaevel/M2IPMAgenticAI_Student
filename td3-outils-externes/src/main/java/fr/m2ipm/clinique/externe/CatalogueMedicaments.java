package fr.m2ipm.clinique.externe;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Accès en lecture au catalogue externe. Requêtes paramétrées uniquement : jamais de SQL écrit par le modèle. */
@Component
public class CatalogueMedicaments {

    public record Fiche(String nom, String principeActif, String contreIndications, String dose) {}

    private final JdbcClient jdbc;

    public CatalogueMedicaments(@Qualifier("catalogue") JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Médicaments dont le nom contient le texte, pour l'espèce donnée (dose adaptée à l'espèce). */
    public List<Fiche> chercher(String nom, String espece) {
        return jdbc.sql("""
                        SELECT m.nom, m.principe_actif, m.contre_indications, e.dose
                        FROM medicament m JOIN medicament_espece e ON e.medicament_id = m.id
                        WHERE LOWER(m.nom) LIKE LOWER(:nom) AND LOWER(e.espece) = LOWER(:espece)
                        ORDER BY m.nom
                        """)
                .param("nom", "%" + (nom == null ? "" : nom.trim()) + "%")
                .param("espece", espece == null ? "" : espece.trim())
                .query((rs, i) -> new Fiche(rs.getString("nom"), rs.getString("principe_actif"),
                        rs.getString("contre_indications"), rs.getString("dose")))
                .list();
    }
}
