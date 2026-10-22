package com.madelmade.repo;

import com.madelmade.domain.Video;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VideoRepository extends JpaRepository<Video, Long> {
    @Query("select v.updatedAt from Video v where v.id = :id")
    Optional<Instant> findUpdatedAt(Long id);
}
