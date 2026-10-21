package com.madelmade.service;

import com.madelmade.repo.PhotoRepository;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Photo addresses carry a version, like /photos/12?v=1729350000000, that changes whenever the
 * photo is edited. Browsers can keep a photo for a long time and still see an edit right away.
 * Templates use it as {@code photoUrls.src(id)}; it is added to every page's model.
 */
@Component("photoUrls")
public class PhotoUrls {
    private final PhotoRepository photos;
    private final Map<Long, Long> versions = new ConcurrentHashMap<>();

    public PhotoUrls(PhotoRepository photos) {
        this.photos = photos;
    }

    public String src(Long id) {
        if (id == null) return "";
        long v = versions.computeIfAbsent(id, k -> photos.findUpdatedAt(k).map(Instant::toEpochMilli).orElse(0L));
        return "/photos/" + id + "?v=" + v;
    }

    /** Called after a photo is saved so its address changes. */
    public void changed(Long id, Instant updatedAt) {
        if (id != null) versions.put(id, updatedAt.toEpochMilli());
    }
}
