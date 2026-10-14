package com.madelmade.repo;

import com.madelmade.domain.Customer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmailIgnoreCase(String email);

    List<Customer> findAllByOrderByCreatedAtDesc();
}
