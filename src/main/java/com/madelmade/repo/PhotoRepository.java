package com.madelmade.repo;

import com.madelmade.domain.Photo;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    /** Just the edited image, so serving a photo never loads the (large) original. */
    interface EditedView {
        byte[] getEdited();
        String getEditedType();
        Instant getUpdatedAt();
    }

    interface OriginalView {
        byte[] getOriginal();
        String getOriginalType();
    }

    @Query("select p.edited as edited, p.editedType as editedType, p.updatedAt as updatedAt from Photo p where p.id = ?1")
    Optional<EditedView> findEdited(Long id);

    @Query("select p.updatedAt from Photo p where p.id = ?1")
    Optional<Instant> findUpdatedAt(Long id);

    @Query("select p.original as original, p.originalType as originalType from Photo p where p.id = ?1")
    Optional<OriginalView> findOriginal(Long id);
}
