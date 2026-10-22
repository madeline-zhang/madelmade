package com.madelmade.service;

import com.madelmade.domain.HeroSlide;
import com.madelmade.domain.HeroVideo;
import com.madelmade.repo.HeroSlideRepository;
import com.madelmade.repo.HeroVideoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The home page hero: photos and looping videos shown in turn, with the time on each and the fade between
 * them set under Admin › Home page.
 */
@Service
public class HeroService {
    static final int MAX_VIDEO = 25 * 1024 * 1024;

    /** What the home page needs for one slide. phone is the phone crop, or the computer photo when there is none. */
    public record Slide(String computer, String phone, String videoUrl, String alt) {
        public boolean isVideo() { return videoUrl != null; }
    }

    private final HeroSlideRepository slides;
    private final HeroVideoRepository videos;
    private final PhotoUrls photoUrls;
    private final PhotoService photos;
    private final SettingsService settings;

    public HeroService(HeroSlideRepository slides, HeroVideoRepository videos, PhotoUrls photoUrls, PhotoService photos,
                       SettingsService settings) {
        this.slides = slides;
        this.videos = videos;
        this.photoUrls = photoUrls;
        this.photos = photos;
        this.settings = settings;
    }

    public List<Slide> slides() {
        return slides.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .filter(s -> s.getVideoId() != null || s.getPhotoId() != null)
                .map(s -> new Slide(
                        s.getPhotoId() == null ? null : photoUrls.src(s.getPhotoId()),
                        s.getPhonePhotoId() != null ? photoUrls.src(s.getPhonePhotoId())
                                : s.getPhotoId() == null ? null : photoUrls.src(s.getPhotoId()),
                        s.getVideoId() == null ? null : videoUrl(s.getVideoId()),
                        s.getAlt() == null ? "" : s.getAlt()))
                .toList();
    }

    public String videoUrl(Long id) {
        return "/hero/video/" + id + "?v=" + videos.findUpdatedAt(id).map(Instant::toEpochMilli).orElse(0L);
    }

    /** Saves an uploaded video, replacing the one already on the slide if there is one. */
    @Transactional
    public HeroVideo saveVideo(HeroSlide slide, byte[] data, String contentType) {
        if (data == null || data.length == 0) throw new UserFacingException("Please choose a video.");
        if (data.length > MAX_VIDEO) throw new UserFacingException("That video is too large (25 MB max). A short clip exported as MP4 is usually well under that.");
        String type = videoType(data, contentType);
        if (type == null) throw new UserFacingException("That file doesn't look like an MP4, MOV or WebM video.");
        HeroVideo v = slide.getVideoId() == null ? new HeroVideo() : videos.findById(slide.getVideoId()).orElseGet(HeroVideo::new);
        v.setData(data);
        v.setContentType(type);
        v.setUpdatedAt(Instant.now());
        v = videos.save(v);
        slide.setVideoId(v.getId());
        return v;
    }

    static String videoType(byte[] b, String declared) {
        if (b.length > 12 && b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') {
            return b[8] == 'q' && b[9] == 't' ? "video/quicktime" : "video/mp4";
        }
        if (b.length > 4 && (b[0] & 0xFF) == 0x1A && (b[1] & 0xFF) == 0x45 && (b[2] & 0xFF) == 0xDF && (b[3] & 0xFF) == 0xA3) return "video/webm";
        return declared != null && declared.startsWith("video/") && declared.length() <= 60 ? declared : null;
    }

    /** Starts the hero with Miriam and the stenciled "Made" loaf, once. Removing them later doesn't bring them back. */
    @Transactional
    public void seed() {
        if (settings.flag("hero.seeded")) return;
        if (slides.count() == 0) {
            PhotoService.EditSettings none = new PhotoService.EditSettings(0, 0, "", 0, 0, 0, 0);
            add(0, "Miriam's sourdough loaf with a diagonal leaf score",
                    photos.fromClasspath("static/img/hero.jpg", "static/img/hero.jpg", none).getId(),
                    photos.fromClasspath("static/img/hero-phone.jpg", "static/img/hero-phone.jpg", none).getId());
            // The original is kept so the crops can be redone in the photo editor.
            add(1, "Sourdough loaf stenciled with MadelMade in script",
                    photos.fromClasspath("seed/hero/made-original.jpg", "seed/hero/made-computer.jpg",
                            new PhotoService.EditSettings(0, 0, "0.1653,0.4596,0.7606,0.3209", 15, 5, 0, 0)).getId(),
                    photos.fromClasspath("seed/hero/made-original.jpg", "seed/hero/made-phone.jpg",
                            new PhotoService.EditSettings(0, 0, "0.2976,0.4464,0.4960,0.5531", 15, 5, 0, 0)).getId());
        }
        settings.putAll(Map.of("hero.seeded", "true"));
    }

    private void add(int order, String alt, Long photoId, Long phonePhotoId) {
        HeroSlide s = new HeroSlide();
        s.setDisplayOrder(order);
        s.setAlt(alt);
        s.setPhotoId(photoId);
        s.setPhonePhotoId(phonePhotoId);
        slides.save(s);
    }
}
