package pt.ulisboa.tecnico.rnl.dei.ems.subject.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.rnl.dei.ems.subject.domain.Subject;

// Repository interface for managing Subject entities
@Repository
@Transactional
public interface SubjectRepository extends JpaRepository<Subject, Long> {

	Optional<Subject> findByCode(String code);

	boolean existsByCode(String code);
}
