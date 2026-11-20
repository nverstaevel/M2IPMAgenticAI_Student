package fr.m2ipm.clinique.externe;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Ce dont la clinique a besoin d'un agenda externe : le reste (Google, simulé) est un détail d'adaptateur. */
public interface AgendaPort {

    /** Créneaux libres de 30 minutes entre 9 h et 18 h (heures de début). */
    List<LocalTime> creneauxLibres(LocalDate jour);

    /** Crée un événement sans invité ; renvoie son identifiant. */
    String creerEvenement(String titre, LocalDateTime debut, int dureeMinutes);
}
