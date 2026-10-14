package com.madelmade.web;

import com.madelmade.domain.Category;
import com.madelmade.domain.Product;
import com.madelmade.repo.CategoryRepository;
import com.madelmade.repo.GalleryPhotoRepository;
import com.madelmade.repo.ProductRepository;
import com.madelmade.service.SettingsService;
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

    public ShopController(ProductRepository products, CategoryRepository categories, GalleryPhotoRepository gallery,
                          SettingsService settings) {
        this.settings = settings;
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
        return "shop/home";
    }

    @GetMapping("/p/{slug}")
    public String product(@PathVariable String slug, Model model) {
        Product p = products.findBySlug(slug).filter(Product::isActive).orElseThrow(NotFoundException::new);
        model.addAttribute("product", p);
        model.addAttribute("certPrice", settings.certificatePrice().toPlainString());
        for (String k : new String[] {"form_letters", "form_notes", "form_certName", "form_certMessage"}) {
            if (!model.containsAttribute(k)) model.addAttribute(k, "");
        }
        if (!model.containsAttribute("form_cert")) model.addAttribute("form_cert", false);
        if (!model.containsAttribute("selected")) model.addAttribute("selected", List.of());
        return "shop/product";
    }

    @GetMapping("/gallery")
    public String gallery(Model model) {
        model.addAttribute("photos", gallery.findAllByOrderByDisplayOrderAscIdAsc());
        return "shop/gallery";
    }

    @GetMapping("/about")
    public String about() {
        return "shop/about";
    }
}
