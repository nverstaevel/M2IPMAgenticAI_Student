package fr.m2ipm.clinique.repository;

import fr.m2ipm.clinique.domain.Consultation;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    List<Consultation> findByCreatureIdOrderByDateHeureDesc(Long creatureId);

    /** Vrai s'il existe une consultation du soigneur qui commence strictement entre les deux bornes. */
    boolean existsBySoigneurIdAndDateHeureGreaterThanAndDateHeureLessThan(
            Long soigneurId, LocalDateTime apres, LocalDateTime avant);
}
