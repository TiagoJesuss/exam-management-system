package pt.ulisboa.tecnico.rnl.dei.ems.question.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.rnl.dei.ems.question.domain.Question;

// Repository interface for managing Question entities
@Repository
@Transactional
public interface QuestionRepository extends JpaRepository<Question, Long> {
	long countByExam_Id(long examId);

	List<Question> findByExam_IdOrderByNumberAsc(long examId);

	Optional<Question> findByExam_IdAndNumber(long examId, int number);
}
