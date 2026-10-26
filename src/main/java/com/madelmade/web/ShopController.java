package com.madelmade.web;

import com.madelmade.domain.Category;
import com.madelmade.domain.Product;
import com.madelmade.repo.CategoryRepository;
import com.madelmade.repo.GalleryPhotoRepository;
import com.madelmade.repo.ProcessStepRepository;
import com.madelmade.repo.ProductRepository;
import com.madelmade.service.HeroService;
import com.madelmade.service.SettingsService;
import com.madelmade.service.VideoService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ShopController {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final GalleryPhotoRepository gallery;
    private final SettingsService settings;
    private final HeroService hero;
    private final ProcessStepRepository steps;
    private final VideoService videos;

    public ShopController(ProductRepository products, CategoryRepository categories, GalleryPhotoRepository gallery,
                          SettingsService settings, HeroService hero, ProcessStepRepository steps, VideoService videos) {
        this.steps = steps;
        this.videos = videos;
        this.settings = settings;
        this.hero = hero;
        this.products = products;
        this.categories = categories;
        this.gallery = gallery;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<Product> menu = products.findMenu();
        List<Category> used = categories.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .filter(c -> menu.stream().anyMatch(p -> p.getCategory().getId().equals(c.getId())))
                .toList();
        model.addAttribute("products", menu);
        model.addAttribute("categories", used);
        model.addAttribute("heroSlides", hero.slides());
        return "shop/home";
    }

    @GetMapping("/p/{slug}")
    public String product(@PathVariable String slug, Model model) {
        Product p = products.findBySlug(slug).filter(Product::isActive).orElseThrow(NotFoundException::new);
        model.addAttribute("product", p);
        model.addAttribute("certPrice", settings.certificatePrice().toPlainString());
        for (String k : new String[] {"form_letters", "form_font", "form_notes", "form_certName", "form_certMessage"}) {
            if (!model.containsAttribute(k)) model.addAttribute(k, "");
        }
        if (!model.containsAttribute("form_cert")) model.addAttribute("form_cert", false);
        if (!model.containsAttribute("selected")) model.addAttribute("selected", List.of());
        return "shop/product";
    }

    @GetMapping("/gallery")
    public String gallery(Model model) {
        model.addAttribute("photos", gallery.findBySectionOrderByDisplayOrderAscIdAsc(com.madelmade.domain.GalleryPhoto.LOAVES));
        model.addAttribute("otherPhotos", gallery.findBySectionOrderByDisplayOrderAscIdAsc(com.madelmade.domain.GalleryPhoto.OTHER));
        return "shop/gallery";
    }

    @GetMapping("/how-its-made")
    public String process(Model model) {
        var list = steps.findAllByOrderByDisplayOrderAscIdAsc();
        // Hidden until the first step is added.
        if (list.isEmpty()) throw new NotFoundException();
        model.addAttribute("steps", list);
        model.addAttribute("videos", videos);
        return "shop/how-its-made";
    }

    @GetMapping("/about")
    public String about() {
        return "shop/about";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "shop/privacy";
    }
}
