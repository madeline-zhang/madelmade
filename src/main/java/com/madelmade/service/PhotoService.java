package com.madelmade.service;

import com.madelmade.domain.Photo;
import com.madelmade.repo.PhotoRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import javax.imageio.ImageIO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Photos are edited in the browser (rotate, tilt, crop, warmth, brightness) so the preview is
 * exactly what gets saved. The original is always kept with the settings, so edits can be redone.
 */
@Service
public class PhotoService {
    private static final int MAX_ORIGINAL = 25 * 1024 * 1024;
    private static final int MAX_EDITED = 6 * 1024 * 1024;

    public record EditSettings(int rotate, double tilt, String crop, int warmth, int brightness) {}

    private final PhotoRepository repo;

    public PhotoService(PhotoRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public Photo create(byte[] original, String originalType, byte[] edited, EditSettings s) {
        Photo p = new Photo();
        if (original == null || original.length == 0) throw new UserFacingException("Please choose a photo.");
        if (original.length > MAX_ORIGINAL) throw new UserFacingException("That photo is too large (25 MB max).");
        p.setOriginal(original);
        p.setOriginalType(sniffType(original, originalType));
        apply(p, edited, s);
        return repo.save(p);
    }

    @Transactional
    public Photo update(Photo p, byte[] edited, EditSettings s) {
        apply(p, edited, s);
        p.setUpdatedAt(Instant.now());
        return repo.save(p);
    }

    private void apply(Photo p, byte[] edited, EditSettings s) {
        if (edited == null || edited.length == 0) throw new UserFacingException("The edited photo didn't come through. Please try again.");
        if (edited.length > MAX_EDITED) throw new UserFacingException("The edited photo is too large.");
        BufferedImage img = read(edited);
        if (img == null) throw new UserFacingException("That file doesn't look like a JPEG or PNG photo.");
        p.setEdited(edited);
        p.setEditedType(sniffType(edited, "image/jpeg"));
        p.setWidth(img.getWidth());
        p.setHeight(img.getHeight());
        p.setRotate(((s.rotate() % 360) + 360) % 360);
        p.setTilt(Math.max(-15, Math.min(15, s.tilt())));
        String crop = s.crop() == null || s.crop().isBlank() ? "original" : s.crop().trim();
        p.setCrop(crop.length() > 60 ? crop.substring(0, 60) : crop);
        p.setWarmth(Math.max(0, Math.min(10, s.warmth())));
        p.setBrightness(Math.max(0, Math.min(10, s.brightness())));
    }

    /** Loads one of the bundled starter photos (already warmed) as a new photo. */
    @Transactional
    public Photo fromClasspath(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            byte[] bytes = in.readAllBytes();
            Photo p = new Photo();
            p.setOriginal(bytes);
            p.setOriginalType("image/jpeg");
            apply(p, bytes, new EditSettings(0, 0, "original", 5, 5));
            return repo.save(p);
        } catch (IOException e) {
            throw new IllegalStateException("Missing starter photo " + path, e);
        }
    }

    private static BufferedImage read(byte[] bytes) {
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            return null;
        }
    }

    static String sniffType(byte[] b, String fallback) {
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8) return "image/jpeg";
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
        if (b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[8] == 'W' && b[9] == 'E') return "image/webp";
        if (b.length > 12 && b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') return "image/heic";
        return fallback == null ? "application/octet-stream" : fallback;
    }
}
