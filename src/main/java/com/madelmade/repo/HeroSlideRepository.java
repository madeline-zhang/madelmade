package com.madelmade.repo;

import com.madelmade.domain.HeroSlide;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HeroSlideRepository extends JpaRepository<HeroSlide, Long> {
    List<HeroSlide> findAllByOrderByDisplayOrderAscIdAsc();
}
