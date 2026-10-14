package com.madelmade.repo;

import com.madelmade.domain.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("select p from Product p where p.active = true order by p.category.displayOrder, p.displayOrder, p.id")
    List<Product> findMenu();

    @Query("select p from Product p order by p.category.displayOrder, p.displayOrder, p.id")
    List<Product> findAllOrdered();

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
