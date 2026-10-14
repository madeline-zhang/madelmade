package com.madelmade.repo;

import com.madelmade.domain.LoginCode;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginCodeRepository extends JpaRepository<LoginCode, Long> {
    Optional<LoginCode> findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    long countByEmailAndCreatedAtAfter(String email, Instant since);

    void deleteByExpiresAtBefore(Instant cutoff);
}
