package com.madelmade.service;

import com.madelmade.domain.Customer;
import com.madelmade.repo.CustomerRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {
    private final CustomerRepository repo;

    public CustomerService(CustomerRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public Customer findOrCreate(String email, String name) {
        String clean = email.trim().toLowerCase();
        return repo.findByEmailIgnoreCase(clean).orElseGet(() -> {
            Customer c = new Customer();
            c.setEmail(clean);
            c.setName(name);
            return repo.save(c);
        });
    }

    @Transactional
    public Customer recordSignIn(String email, String name, String googleId) {
        Customer c = findOrCreate(email, name);
        if ((c.getName() == null || c.getName().isBlank()) && name != null) c.setName(name);
        if (googleId != null) c.setGoogleId(googleId);
        c.setLastSignIn(Instant.now());
        return repo.save(c);
    }
}
