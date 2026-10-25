package com.madelmade.repo;

import com.madelmade.domain.ProcessStep;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessStepRepository extends JpaRepository<ProcessStep, Long> {
    List<ProcessStep> findAllByOrderByDisplayOrderAscIdAsc();
}
