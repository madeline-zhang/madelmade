package com.madelmade.service;

import com.madelmade.domain.Video;
import com.madelmade.repo.VideoRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Short looping videos for the home page and How it's made. Stored in the database like photos. */
@Service
public class VideoService {
    static final int MAX_VIDEO = 25 * 1024 * 1024;

    private final VideoRepository videos;

    public VideoService(VideoRepository videos) {
        this.videos = videos;
    }

    public String url(Long id) {
        return "/videos/" + id + "?v=" + videos.findUpdatedAt(id).map(Instant::toEpochMilli).orElse(0L);
    }

    /** Saves an upload, replacing the video with existingId when there is one. */
    @Transactional
    public Video save(Long existingId, byte[] data, String contentType) {
        if (data == null || data.length == 0) throw new UserFacingException("Please choose a video.");
        if (data.length > MAX_VIDEO) throw new UserFacingException("That video is too large (25 MB max). A short clip exported as MP4 is usually well under that.");
        String type = type(data, contentType);
        if (type == null) throw new UserFacingException("That file doesn't look like an MP4, MOV or WebM video.");
        Video v = existingId == null ? new Video() : videos.findById(existingId).orElseGet(Video::new);
        v.setData(data);
        v.setContentType(type);
        v.setUpdatedAt(Instant.now());
        return videos.save(v);
    }

    static String type(byte[] b, String declared) {
        if (b.length > 12 && b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') {
            return b[8] == 'q' && b[9] == 't' ? "video/quicktime" : "video/mp4";
        }
        if (b.length > 4 && (b[0] & 0xFF) == 0x1A && (b[1] & 0xFF) == 0x45 && (b[2] & 0xFF) == 0xDF && (b[3] & 0xFF) == 0xA3) return "video/webm";
        return declared != null && declared.startsWith("video/") && declared.length() <= 60 ? declared : null;
    }
}
