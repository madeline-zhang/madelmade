package com.madelmade.repo;

import com.madelmade.domain.GalleryPhoto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GalleryPhotoRepository extends JpaRepository<GalleryPhoto, Long> {
    List<GalleryPhoto> findAllByOrderByDisplayOrderAscIdAsc();
}
