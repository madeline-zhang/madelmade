package com.madelmade.web.admin;

import com.madelmade.domain.GalleryPhoto;
import com.madelmade.repo.GalleryPhotoRepository;
import com.madelmade.web.NotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/gallery")
public class AdminGalleryController {
    private final GalleryPhotoRepository gallery;

    public AdminGalleryController(GalleryPhotoRepository gallery) {
        this.gallery = gallery;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("photos", gallery.findBySectionOrderByDisplayOrderAscIdAsc(GalleryPhoto.LOAVES));
        model.addAttribute("otherPhotos", gallery.findBySectionOrderByDisplayOrderAscIdAsc(GalleryPhoto.OTHER));
        return "admin/gallery";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @RequestParam(required = false) String caption,
                         @RequestParam(defaultValue = "0") int displayOrder, RedirectAttributes flash) {
        GalleryPhoto g = gallery.findById(id).orElseThrow(NotFoundException::new);
        g.setCaption(caption == null || caption.isBlank() ? null : caption.trim());
        g.setDisplayOrder(displayOrder);
        gallery.save(g);
        flash.addFlashAttribute("ok", "Saved.");
        return "redirect:/admin/gallery" + (g.isOther() ? "#other" : "");
    }
}
