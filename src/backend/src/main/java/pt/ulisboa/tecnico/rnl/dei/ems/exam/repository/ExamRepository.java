package pt.ulisboa.tecnico.rnl.dei.ems.exam.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.rnl.dei.ems.exam.domain.Exam;

// Repository interface for managing Exam entities
@Repository
@Transactional
public interface ExamRepository extends JpaRepository<Exam, Long> {
}
