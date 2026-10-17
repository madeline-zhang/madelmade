package com.madelmade.repo;

import com.madelmade.domain.PromoCode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    Optional<PromoCode> findByCodeIgnoreCase(String code);

    List<PromoCode> findAllByOrderByActiveDescCreatedAtDesc();
}
