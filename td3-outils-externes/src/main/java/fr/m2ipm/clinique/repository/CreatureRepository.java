package fr.m2ipm.clinique.repository;

import fr.m2ipm.clinique.domain.Creature;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatureRepository extends JpaRepository<Creature, Long> {
    List<Creature> findByProprietaireIdOrderByNom(Long proprietaireId);
}
