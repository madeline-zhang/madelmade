package com.madelmade.web.admin;

import com.madelmade.domain.GalleryPhoto;
import com.madelmade.domain.OptionChoice;
import com.madelmade.domain.Photo;
import com.madelmade.domain.Product;
import com.madelmade.repo.GalleryPhotoRepository;
import com.madelmade.repo.OptionChoiceRepository;
import com.madelmade.repo.PhotoRepository;
import com.madelmade.repo.ProductRepository;
import com.madelmade.service.PhotoService;
import com.madelmade.service.SettingsService;
import com.madelmade.service.UserFacingException;
import com.madelmade.web.NotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Photo editor: rotate, tilt, crop, warmth and brightness. Edits are made in the browser and the
 * original is always kept, so a photo can be re-edited later without losing quality.
 * Targets: product-ID, choice-ID, gallery-ID or gallery-new.
 */
@Controller
@RequestMapping("/admin/photos")
public class AdminPhotosController {
    private final PhotoRepository photos;
    private final PhotoService photoService;
    private final ProductRepository products;
    private final OptionChoiceRepository choices;
    private final GalleryPhotoRepository gallery;
    private final SettingsService settings;

    public AdminPhotosController(PhotoRepository photos, PhotoService photoService, ProductRepository products,
                                 OptionChoiceRepository choices, GalleryPhotoRepository gallery, SettingsService settings) {
        this.photos = photos;
        this.photoService = photoService;
        this.products = products;
        this.choices = choices;
        this.gallery = gallery;
        this.settings = settings;
    }

    record Target(String key, String title, Long photoId, String back, String crop) {}

    private Target target(String key) {
        String[] parts = key == null ? new String[0] : key.split("-", 2);
        if (parts.length != 2) throw new NotFoundException();
        switch (parts[0]) {
            case "product" -> {
                Product p = products.findById(parse(parts[1])).orElseThrow(NotFoundException::new);
                return new Target(key, p.getName(), p.getPhotoId(), "/admin/products/" + p.getId(), "square");
            }
            case "choice" -> {
                OptionChoice c = choices.findById(parse(parts[1])).orElseThrow(NotFoundException::new);
                return new Target(key, c.getGroup().getProduct().getName() + ": " + c.getName(), c.getPhotoId(),
                        "/admin/products/" + c.getGroup().getProduct().getId() + "#group-" + c.getGroup().getId(), "square");
            }
            case "gallery" -> {
                if (parts[1].equals("new")) return new Target(key, "New gallery photo", null, "/admin/gallery", "portrait");
                GalleryPhoto g = gallery.findById(parse(parts[1])).orElseThrow(NotFoundException::new);
                return new Target(key, g.getCaption() == null ? "Gallery photo" : g.getCaption(), g.getPhotoId(), "/admin/gallery", "portrait");
            }
            default -> throw new NotFoundException();
        }
    }

    @GetMapping("/edit")
    public String edit(@RequestParam String target, Model model) {
        Target t = target(target);
        Photo p = t.photoId() == null ? null : photos.findById(t.photoId()).orElse(null);
        model.addAttribute("target", t);
        model.addAttribute("photo", p);
        model.addAttribute("defaultWarmth", p != null ? p.getWarmth() : settings.number("photos.warmth"));
        model.addAttribute("defaultBrightness", p != null ? p.getBrightness() : settings.number("photos.brightness"));
        model.addAttribute("defaultCrop", p != null ? p.getCrop() : t.crop());
        model.addAttribute("defaultRotate", p != null ? p.getRotate() : 0);
        model.addAttribute("defaultTilt", p != null ? p.getTilt() : 0.0);
        model.addAttribute("section", target.startsWith("gallery") ? "gallery" : "products");
        return "admin/photo-editor";
    }

    @GetMapping("/{id}/original")
    public ResponseEntity<byte[]> original(@PathVariable Long id) {
        PhotoRepository.OriginalView v = photos.findOriginal(id).orElseThrow(NotFoundException::new);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(v.getOriginalType())).body(v.getOriginal());
    }

    @PostMapping("/save")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, String>> save(@RequestParam String target,
                                                    @RequestParam(required = false) MultipartFile original,
                                                    @RequestParam MultipartFile edited,
                                                    @RequestParam(defaultValue = "0") int rotate,
                                                    @RequestParam(defaultValue = "0") double tilt,
                                                    @RequestParam(defaultValue = "original") String crop,
                                                    @RequestParam(defaultValue = "5") int warmth,
                                                    @RequestParam(defaultValue = "5") int brightness,
                                                    @RequestParam(required = false) String caption) throws IOException {
        Target t = target(target);
        PhotoService.EditSettings s = new PhotoService.EditSettings(rotate, tilt, crop, warmth, brightness);
        try {
            Photo photo;
            boolean replacing = original != null && !original.isEmpty();
            if (!replacing && t.photoId() != null) {
                photo = photoService.update(photos.findById(t.photoId()).orElseThrow(NotFoundException::new), edited.getBytes(), s);
            } else {
                if (!replacing) throw new UserFacingException("Please choose a photo.");
                // A new upload gets its own photo, so other items that used the old one are untouched.
                photo = photoService.create(original.getBytes(), original.getContentType(), edited.getBytes(), s);
            }
            attach(t, photo.getId(), caption);
            return ResponseEntity.ok(Map.of("redirect", t.back()));
        } catch (UserFacingException e) {
            return ResponseEntity.badRequest().body(Map.of("error", String.join(" ", e.getMessages())));
        }
    }

    @PostMapping("/remove")
    @Transactional
    public String remove(@RequestParam String target, RedirectAttributes flash) {
        Target t = target(target);
        String[] parts = target.split("-", 2);
        switch (parts[0]) {
            case "product" -> products.findById(parse(parts[1])).ifPresent(p -> { p.setPhotoId(null); products.save(p); });
            case "choice" -> choices.findById(parse(parts[1])).ifPresent(c -> { c.setPhotoId(null); choices.save(c); });
            case "gallery" -> gallery.findById(parse(parts[1])).ifPresent(gallery::delete);
            default -> { }
        }
        flash.addFlashAttribute("ok", "Photo removed.");
        return "redirect:" + t.back();
    }

    private void attach(Target t, Long photoId, String caption) {
        String[] parts = t.key().split("-", 2);
        switch (parts[0]) {
            case "product" -> products.findById(parse(parts[1])).ifPresent(p -> {
                p.setPhotoId(photoId);
                p.setUpdatedAt(Instant.now());
                products.save(p);
            });
            case "choice" -> choices.findById(parse(parts[1])).ifPresent(c -> { c.setPhotoId(photoId); choices.save(c); });
            case "gallery" -> {
                GalleryPhoto g = parts[1].equals("new") ? new GalleryPhoto()
                        : gallery.findById(parse(parts[1])).orElseThrow(NotFoundException::new);
                if (parts[1].equals("new")) g.setDisplayOrder((int) gallery.count());
                g.setPhotoId(photoId);
                if (caption != null) g.setCaption(caption.isBlank() ? null : caption.trim());
                gallery.save(g);
            }
            default -> throw new NotFoundException();
        }
    }

    private static Long parse(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new NotFoundException();
        }
    }
}
