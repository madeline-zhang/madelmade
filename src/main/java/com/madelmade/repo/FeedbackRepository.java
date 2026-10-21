package com.madelmade.repo;

import com.madelmade.domain.Feedback;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByKindOrderByResolvedAscCreatedAtDesc(String kind);

    long countByResolvedFalse();

    long countByKindAndResolvedFalse(String kind);
}
