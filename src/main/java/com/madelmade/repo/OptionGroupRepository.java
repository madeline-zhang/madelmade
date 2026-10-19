package com.madelmade.repo;

import com.madelmade.domain.OptionGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OptionGroupRepository extends JpaRepository<OptionGroup, Long> {
    /** Shared groups, like Flavor, that several products use. */
    List<OptionGroup> findByProductIsNullOrderByDisplayOrderAscIdAsc();
}
