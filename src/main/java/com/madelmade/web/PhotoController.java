package com.madelmade.web;

import com.madelmade.repo.PhotoRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.WebRequest;

@Controller
public class PhotoController {
    private final PhotoRepository photos;

    public PhotoController(PhotoRepository photos) {
        this.photos = photos;
    }

    @GetMapping("/photos/{id}")
    public ResponseEntity<byte[]> photo(@PathVariable Long id, WebRequest request) {
        PhotoRepository.EditedView v = photos.findEdited(id).orElseThrow(NotFoundException::new);
        String etag = "\"" + id + "-" + v.getUpdatedAt().toEpochMilli() + "\"";
        if (request.checkNotModified(etag)) return null;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(v.getEditedType()))
                // Re-edited photos keep their address, so browsers must check back each time.
                // An unchanged photo answers "not modified" without sending the picture again.
                .cacheControl(CacheControl.noCache().cachePublic())
                .eTag(etag)
                .body(v.getEdited());
    }
}
