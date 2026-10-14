package com.madelmade.repo;

import com.madelmade.domain.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderByDisplayOrderAscIdAsc();

    Optional<Category> findBySlug(String slug);
}
