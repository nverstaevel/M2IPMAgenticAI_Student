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
        // TODO TD3 : à écrire
        throw new UnsupportedOperationException("À écrire : creneauxLibres");
    }

    @Override
    public String creerEvenement(String titre, LocalDateTime debut, int dureeMinutes) {
        // TODO TD3 : à écrire
        throw new UnsupportedOperationException("À écrire : creerEvenement");
    }

    public List<Evenement> evenementsCrees() {
        // TODO TD3 : à écrire
        throw new UnsupportedOperationException("À écrire : evenementsCrees");
    }
}
