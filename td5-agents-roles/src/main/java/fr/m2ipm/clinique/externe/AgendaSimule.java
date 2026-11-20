package fr.m2ipm.clinique.externe;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/** Agenda en mémoire pour les tests et le travail hors ligne. Quelques créneaux sont déjà occupés. */
public class AgendaSimule implements AgendaPort {

    public record Evenement(String id, String titre, LocalDateTime debut, int dureeMinutes) {}

    private final Set<LocalDateTime> occupes = new CopyOnWriteArraySet<>();
    private final List<Evenement> crees = new CopyOnWriteArrayList<>();

    public AgendaSimule() {
        occupes.add(LocalDateTime.of(2026, 12, 3, 10, 0));
        occupes.add(LocalDateTime.of(2026, 12, 3, 14, 30));
    }

    @Override
    public List<LocalTime> creneauxLibres(LocalDate jour) {
        List<LocalTime> libres = new ArrayList<>();
        for (LocalTime h = LocalTime.of(9, 0); h.isBefore(LocalTime.of(18, 0)); h = h.plusMinutes(30)) {
            if (!occupes.contains(jour.atTime(h))) {
                libres.add(h);
            }
        }
        return libres;
    }

    @Override
    public String creerEvenement(String titre, LocalDateTime debut, int dureeMinutes) {
        String id = "sim-" + (crees.size() + 1);
        crees.add(new Evenement(id, titre, debut, dureeMinutes));
        occupes.add(debut);
        return id;
    }

    public List<Evenement> evenementsCrees() {
        return List.copyOf(crees);
    }
}
