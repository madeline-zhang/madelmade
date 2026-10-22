package com.madelmade.web.admin;

import com.madelmade.domain.GalleryPhoto;
import com.madelmade.domain.HeroSlide;
import com.madelmade.domain.ProcessStep;
import com.madelmade.domain.OptionChoice;
import com.madelmade.domain.OptionGroup;
import com.madelmade.domain.Photo;
import com.madelmade.domain.Product;
import com.madelmade.repo.GalleryPhotoRepository;
import com.madelmade.repo.HeroSlideRepository;
import com.madelmade.repo.ProcessStepRepository;
import com.madelmade.repo.VideoRepository;
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
 * Targets: product-ID, choice-ID, gallery-ID, gallery-new, other-new (Other bakes), about-photo, cert-photo,
 * hero-ID, hero-new or heroPhone-ID (home page photos), step-ID (How it's made).
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
    private final HeroSlideRepository heroSlides;
    private final ProcessStepRepository processSteps;
    private final VideoRepository videos;

    public AdminPhotosController(PhotoRepository photos, PhotoService photoService, ProductRepository products,
                                 OptionChoiceRepository choices, GalleryPhotoRepository gallery, SettingsService settings,
                                 HeroSlideRepository heroSlides, ProcessStepRepository processSteps, VideoRepository videos) {
        this.heroSlides = heroSlides;
        this.processSteps = processSteps;
        this.videos = videos;
        this.photos = photos;
        this.photoService = photoService;
        this.products = products;
        this.choices = choices;
        this.gallery = gallery;
        this.settings = settings;
    }

    /** copyOriginal: the photo to start from belongs to something else, so saving makes a new photo from its original. */
    record Target(String key, String title, Long photoId, String back, String crop, boolean copyOriginal) {
        Target(String key, String title, Long photoId, String back, String crop) {
            this(key, title, photoId, back, crop, false);
        }
    }

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
                OptionGroup g = c.getGroup();
                if (g.isShared()) {
                    return new Target(key, g.getName() + ": " + c.getName(), c.getPhotoId(), "/admin/options#group-" + g.getId(), "square");
                }
                return new Target(key, g.getProduct().getName() + ": " + c.getName(), c.getPhotoId(),
                        "/admin/products/" + g.getProduct().getId() + "#group-" + g.getId(), "square");
            }
            case "gallery" -> {
                if (parts[1].equals("new")) return new Target(key, "New gallery photo", null, "/admin/gallery", "portrait");
                GalleryPhoto g = gallery.findById(parse(parts[1])).orElseThrow(NotFoundException::new);
                return new Target(key, g.getCaption() == null ? "Gallery photo" : g.getCaption(), g.getPhotoId(), "/admin/gallery", "portrait");
            }
            case "other" -> {
                if (!parts[1].equals("new")) throw new NotFoundException();
                return new Target(key, "New photo for Other bakes", null, "/admin/gallery#other", "portrait");
            }
            case "about" -> {
                if (!parts[1].equals("photo")) throw new NotFoundException();
                return new Target(key, "Your photo on the About page", settings.aboutPhotoId(), "/admin/pages", "portrait");
            }
            case "cert" -> {
                if (!parts[1].equals("photo")) throw new NotFoundException();
                return new Target(key, "Example birth certificate", settings.certPhotoId(), "/admin/settings#certificates", "certificate");
            }
            case "hero" -> {
                if (parts[1].equals("new")) return new Target(key, "New home page photo", null, "/admin/hero", "hero");
                HeroSlide h = heroSlides.findById(parse(parts[1])).orElseThrow(NotFoundException::new);
                return new Target(key, "Home page photo, computer crop", h.getPhotoId(), "/admin/hero#slide-" + h.getId(), "hero");
            }
            case "step" -> {
                ProcessStep st = processSteps.findById(parse(parts[1])).orElseThrow(NotFoundException::new);
                return new Target(key, "How it's made: " + st.getTitle(), st.getPhotoId(), "/admin/process#step-" + st.getId(), "portrait");
            }
            case "heroPhone" -> {
                HeroSlide h = heroSlides.findById(parse(parts[1])).orElseThrow(NotFoundException::new);
                // Until it has its own phone crop, start from the computer photo's original.
                boolean own = h.getPhonePhotoId() != null;
                return new Target(key, "Home page photo, phone crop", own ? h.getPhonePhotoId() : h.getPhotoId(),
                        "/admin/hero#slide-" + h.getId(), "heroPhone", !own);
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
        model.addAttribute("defaultContrast", p != null ? p.getContrast() : settings.number("photos.contrast"));
        model.addAttribute("defaultSaturation", p != null ? p.getSaturation() : settings.number("photos.saturation"));
        model.addAttribute("defaultCrop", p != null ? p.getCrop() : "");
        // Menu cards and swatches are square; gallery and About photos are 4:5; the certificate card is 4:3 landscape.
        model.addAttribute("aspect", switch (t.crop()) {
            case "square" -> 1.0;
            case "certificate" -> 4.0 / 3;
            case "hero" -> 16.0 / 9;
            case "heroPhone" -> 390.0 / 580;
            default -> 0.8;
        });
        model.addAttribute("aspectLabel", switch (t.crop()) {
            case "square" -> "Square, to match the menu";
            case "certificate" -> "4:3, landscape like the certificate card";
            case "hero" -> "16:9, wide for computer screens. The headline sits over the left side.";
            case "heroPhone" -> "Tall, for phones. The headline sits over the bottom half.";
            default -> target.startsWith("step") ? "4:5, tall like the How it's made page" : "4:5, to match the gallery";
        });
        model.addAttribute("defaultRotate", p != null ? p.getRotate() : 0);
        model.addAttribute("defaultTilt", p != null ? p.getTilt() : 0.0);
        model.addAttribute("section", target.startsWith("hero") ? "hero" : target.startsWith("step") ? "process"
                : target.startsWith("gallery") || target.startsWith("other") ? "gallery"
                : target.startsWith("about") ? "pages" : target.startsWith("cert") ? "settings" : "products");
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
                                                    @RequestParam(defaultValue = "") String crop,
                                                    @RequestParam(defaultValue = "0") int warmth,
                                                    @RequestParam(defaultValue = "0") int brightness,
                                                    @RequestParam(defaultValue = "0") int contrast,
                                                    @RequestParam(defaultValue = "0") int saturation,
                                                    @RequestParam(required = false) String caption) throws IOException {
        Target t = target(target);
        PhotoService.EditSettings s = new PhotoService.EditSettings(rotate, tilt, crop, warmth, brightness, contrast, saturation);
        try {
            Photo photo;
            boolean replacing = original != null && !original.isEmpty();
            if (!replacing && t.photoId() != null && t.copyOriginal()) {
                Photo source = photos.findById(t.photoId()).orElseThrow(NotFoundException::new);
                photo = photoService.create(source.getOriginal(), source.getOriginalType(), edited.getBytes(), s);
            } else if (!replacing && t.photoId() != null) {
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
            case "about" -> settings.putAll(Map.of("about.photoId", ""));
            case "cert" -> settings.putAll(Map.of("cert.photoId", ""));
            case "heroPhone" -> heroSlides.findById(parse(parts[1])).ifPresent(h -> { h.setPhonePhotoId(null); heroSlides.save(h); });
            case "step" -> processSteps.findById(parse(parts[1])).ifPresent(st -> { st.setPhotoId(null); processSteps.save(st); });
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
                if (parts[1].equals("new")) g.setDisplayOrder((int) gallery.countBySection(GalleryPhoto.LOAVES));
                g.setPhotoId(photoId);
                if (caption != null) g.setCaption(caption.isBlank() ? null : caption.trim());
                gallery.save(g);
            }
            case "other" -> {
                GalleryPhoto g = new GalleryPhoto();
                g.setSection(GalleryPhoto.OTHER);
                g.setDisplayOrder((int) gallery.countBySection(GalleryPhoto.OTHER));
                g.setPhotoId(photoId);
                if (caption != null && !caption.isBlank()) g.setCaption(caption.trim());
                gallery.save(g);
            }
            case "about" -> settings.putAll(Map.of("about.photoId", String.valueOf(photoId)));
            case "cert" -> settings.putAll(Map.of("cert.photoId", String.valueOf(photoId)));
            case "hero" -> {
                HeroSlide h = parts[1].equals("new") ? new HeroSlide() : heroSlides.findById(parse(parts[1])).orElseThrow(NotFoundException::new);
                if (parts[1].equals("new")) {
                    h.setDisplayOrder((int) heroSlides.count());
                    h.setAlt(caption == null || caption.isBlank() ? "A MadelMade sourdough loaf" : caption.trim());
                }
                h.setPhotoId(photoId);
                heroSlides.save(h);
            }
            case "heroPhone" -> heroSlides.findById(parse(parts[1])).ifPresent(h -> { h.setPhonePhotoId(photoId); heroSlides.save(h); });
            case "step" -> processSteps.findById(parse(parts[1])).ifPresent(st -> {
                // A photo replaces a video; the old video is cleaned up with the step's media.
                Long oldVideo = st.getVideoId();
                st.setPhotoId(photoId);
                st.setVideoId(null);
                processSteps.save(st);
                if (oldVideo != null) videos.deleteById(oldVideo);
            });
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
