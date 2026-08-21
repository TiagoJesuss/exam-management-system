package pt.ulisboa.tecnico.rnl.dei.ems.evaluate.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.domain.Evaluate;
import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.domain.Evaluate.EvaluateState;
import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.domain.Evaluate.EvaluateType;

@Repository
@Transactional
public interface EvaluateRepository extends JpaRepository<Evaluate, Long> {

	boolean existsByQuestion_Exam_IdAndType(long examId, EvaluateType type);

	List<Evaluate> findByQuestion_Exam_IdAndType(long examId, EvaluateType type);

	List<Evaluate> findByProfessor_IdAndStateIn(long professorId, Collection<EvaluateState> states);
}
