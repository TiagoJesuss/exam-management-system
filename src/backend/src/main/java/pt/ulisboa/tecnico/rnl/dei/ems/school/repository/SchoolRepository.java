package pt.ulisboa.tecnico.rnl.dei.ems.school.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.rnl.dei.ems.school.domain.School;

// Repository interface for managing School entities
@Repository
@Transactional
public interface SchoolRepository extends JpaRepository<School, Long> {

	Optional<School> findByCode(String code);

	boolean existsByCode(String code);
}
