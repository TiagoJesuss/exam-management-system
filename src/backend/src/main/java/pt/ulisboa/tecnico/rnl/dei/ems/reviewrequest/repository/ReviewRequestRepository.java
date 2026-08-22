package pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.domain.ReviewRequest;
import pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.domain.ReviewRequest.ReviewRequestStatus;

@Repository
@Transactional
public interface ReviewRequestRepository extends JpaRepository<ReviewRequest, Long> {

	boolean existsByQuestion_Id(long questionId);

	boolean existsByQuestion_Exam_Id(long examId);

	List<ReviewRequest> findByQuestion_Exam_Id(long examId);

	Optional<ReviewRequest> findByQuestion_Id(long questionId);

	List<ReviewRequest> findByQuestion_Exam_IdInAndStatus(
			Collection<Long> examIds, ReviewRequestStatus status);
}
