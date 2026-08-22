package pt.ulisboa.tecnico.rnl.dei.ems.exam.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

import pt.ulisboa.tecnico.rnl.dei.ems.exam.domain.Exam;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.domain.Exam.ExamState;

// Repository interface for managing Exam entities
@Repository
@Transactional
public interface ExamRepository extends JpaRepository<Exam, Long> {
	List<Exam> findByStudent_IdAndStateIn(long studentId, Collection<ExamState> states);
}

