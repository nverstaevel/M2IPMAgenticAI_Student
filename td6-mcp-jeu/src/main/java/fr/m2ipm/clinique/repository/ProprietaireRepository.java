package fr.m2ipm.clinique.repository;

import fr.m2ipm.clinique.domain.Proprietaire;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProprietaireRepository extends JpaRepository<Proprietaire, Long> {
    List<Proprietaire> findByNomContainingIgnoreCaseOrderByNom(String nom);
}
