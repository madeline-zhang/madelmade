package com.madelmade.repo;

import com.madelmade.domain.Feedback;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findAllByOrderByResolvedAscCreatedAtDesc();

    long countByResolvedFalse();
}
