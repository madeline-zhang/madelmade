package com.madelmade.web;

import com.madelmade.domain.Video;
import com.madelmade.repo.VideoRepository;
import java.time.Duration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class VideoController {
    private final VideoRepository videos;

    public VideoController(VideoRepository videos) {
        this.videos = videos;
    }

    /** Returned as a resource so browsers can ask for parts of it, which Safari needs to play video. */
    @GetMapping("/videos/{id}")
    public ResponseEntity<Resource> video(@PathVariable Long id, @RequestParam(name = "v", required = false) String version) {
        Video v = videos.findById(id).orElseThrow(NotFoundException::new);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(v.getContentType()))
                .cacheControl(version != null ? CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable()
                        : CacheControl.noCache().cachePublic())
                .body(new ByteArrayResource(v.getData()));
    }
}
