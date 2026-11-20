package fr.m2ipm.clinique.repository;

import fr.m2ipm.clinique.domain.Soigneur;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SoigneurRepository extends JpaRepository<Soigneur, Long> {

    @Query("select distinct s from Soigneur s join s.especesSoignees e "
            + "where lower(e.nom) = lower(:espece) order by s.nom")
    List<Soigneur> findByEspece(@Param("espece") String espece);

    /** Verrou pessimiste : sérialise les planifications concurrentes pour un même soigneur. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Soigneur s where s.id = :id")
    Optional<Soigneur> findByIdPourPlanification(@Param("id") Long id);
}
