package com.madelmade.repo;

import com.madelmade.domain.HeroVideo;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface HeroVideoRepository extends JpaRepository<HeroVideo, Long> {
    @Query("select v.updatedAt from HeroVideo v where v.id = :id")
    Optional<Instant> findUpdatedAt(Long id);
}
