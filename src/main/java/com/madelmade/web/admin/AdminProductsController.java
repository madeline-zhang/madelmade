package com.madelmade.web.admin;

import com.madelmade.domain.Category;
import com.madelmade.domain.OptionChoice;
import com.madelmade.domain.OptionGroup;
import com.madelmade.domain.Product;
import com.madelmade.repo.CategoryRepository;
import com.madelmade.repo.OptionChoiceRepository;
import com.madelmade.repo.OptionGroupRepository;
import com.madelmade.repo.ProductRepository;
import com.madelmade.web.NotFoundException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Add, edit, hide and reorder products and their options. No code change needed for new items. */
@Controller
@RequestMapping("/admin")
public class AdminProductsController {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final OptionGroupRepository groups;
    private final OptionChoiceRepository choices;

    public AdminProductsController(ProductRepository products, CategoryRepository categories,
                                   OptionGroupRepository groups, OptionChoiceRepository choices) {
        this.products = products;
        this.categories = categories;
        this.groups = groups;
        this.choices = choices;
    }

    @GetMapping("/products")
    public String list(Model model) {
        model.addAttribute("products", products.findAllOrdered());
        model.addAttribute("categories", categories.findAllByOrderByDisplayOrderAscIdAsc());
        return "admin/products";
    }

    @GetMapping("/products/new")
    public String create(Model model) {
        Product p = new Product();
        p.setDisplayOrder((int) products.count());
        model.addAttribute("product", p);
        model.addAttribute("categories", categories.findAllByOrderByDisplayOrderAscIdAsc());
        return "admin/product-edit";
    }

    @GetMapping("/products/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("product", find(id));
        model.addAttribute("categories", categories.findAllByOrderByDisplayOrderAscIdAsc());
        return "admin/product-edit";
    }

    @PostMapping("/products/save")
    @Transactional
    public String save(@RequestParam(required = false) Long id, @RequestParam String name,
                       @RequestParam Long categoryId, @RequestParam(required = false) String shortDescription,
                       @RequestParam(required = false) String description, @RequestParam(required = false) String basePrice,
                       @RequestParam(required = false) Integer leadTimeDays, @RequestParam(defaultValue = "0") int displayOrder,
                       @RequestParam(defaultValue = "false") boolean active, @RequestParam(defaultValue = "false") boolean soldOut,
                       @RequestParam(defaultValue = "false") boolean countsAsLoaf,
                       @RequestParam(defaultValue = "false") boolean pickupOnly,
                       @RequestParam(defaultValue = "false") boolean allowsBirthCertificate,
                       @RequestParam(required = false) String badge,
                       @RequestParam(required = false) String ingredients, @RequestParam(required = false) String allergens,
                       @RequestParam(required = false) String notesHint,
                       RedirectAttributes flash) {
        if (name == null || name.isBlank()) {
            flash.addFlashAttribute("errors", List.of("Give the product a name."));
            return id == null ? "redirect:/admin/products/new" : "redirect:/admin/products/" + id;
        }
        Product p = id == null ? new Product() : find(id);
        p.setName(name.trim());
        if (p.getSlug() == null) p.setSlug(uniqueSlug(name));
        p.setCategory(categories.findById(categoryId).orElseThrow(NotFoundException::new));
        p.setShortDescription(blank(shortDescription));
        p.setDescription(blank(description));
        p.setBasePrice(money(basePrice));
        p.setLeadTimeDays(leadTimeDays);
        p.setDisplayOrder(displayOrder);
        p.setActive(active);
        p.setSoldOut(soldOut);
        p.setCountsAsLoaf(countsAsLoaf);
        p.setPickupOnly(pickupOnly);
        p.setAllowsBirthCertificate(allowsBirthCertificate);
        p.setBadge(blank(badge));
        p.setIngredients(blank(ingredients));
        p.setAllergens(blank(allergens));
        p.setNotesHint(blank(notesHint));
        p.setUpdatedAt(Instant.now());
        products.save(p);
        flash.addFlashAttribute("ok", id == null ? "Product added. Now add its options and photo." : "Saved.");
        return "redirect:/admin/products/" + p.getId();
    }

    @PostMapping("/products/{id}/toggle")
    @Transactional
    public String toggle(@PathVariable Long id, @RequestParam String field, RedirectAttributes flash) {
        Product p = find(id);
        if ("active".equals(field)) p.setActive(!p.isActive());
        if ("soldOut".equals(field)) p.setSoldOut(!p.isSoldOut());
        products.save(p);
        flash.addFlashAttribute("ok", p.getName() + (field.equals("active") ? (p.isActive() ? " is on the menu." : " is hidden.")
                : (p.isSoldOut() ? " is marked sold out." : " is available again.")));
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/delete")
    @Transactional
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        Product p = find(id);
        products.delete(p);
        flash.addFlashAttribute("ok", p.getName() + " was deleted. Past orders keep their details.");
        return "redirect:/admin/products";
    }

    // ---- Option groups ----

    @PostMapping("/products/{id}/groups")
    @Transactional
    public String addGroup(@PathVariable Long id, @RequestParam String name,
                           @RequestParam(defaultValue = OptionGroup.STYLE_BUTTONS) String displayStyle,
                           RedirectAttributes flash) {
        Product p = find(id);
        if (name == null || name.isBlank()) return back(id, flash, "Name the option group, e.g. Flavor.");
        OptionGroup g = new OptionGroup();
        g.setProduct(p);
        g.setName(name.trim());
        g.setDisplayStyle(OptionGroup.STYLE_SWATCHES.equals(displayStyle) ? OptionGroup.STYLE_SWATCHES : OptionGroup.STYLE_BUTTONS);
        g.setDisplayOrder(p.getOptionGroups().size());
        p.getOptionGroups().add(g);
        products.save(p);
        flash.addFlashAttribute("ok", "Added " + g.getName() + ". Now add its choices.");
        return "redirect:/admin/products/" + id + "#group-" + g.getId();
    }

    @PostMapping("/groups/{gid}")
    @Transactional
    public String updateGroup(@PathVariable Long gid, @RequestParam String name, @RequestParam String displayStyle,
                              @RequestParam(defaultValue = "0") int displayOrder,
                              @RequestParam(defaultValue = "false") boolean required,
                              @RequestParam(defaultValue = "false") boolean multiple, RedirectAttributes flash) {
        OptionGroup g = groups.findById(gid).orElseThrow(NotFoundException::new);
        if (name != null && !name.isBlank()) g.setName(name.trim());
        g.setDisplayStyle(OptionGroup.STYLE_SWATCHES.equals(displayStyle) ? OptionGroup.STYLE_SWATCHES : OptionGroup.STYLE_BUTTONS);
        g.setDisplayOrder(displayOrder);
        g.setRequired(required);
        g.setMultiple(multiple);
        groups.save(g);
        flash.addFlashAttribute("ok", "Saved " + g.getName() + ".");
        return "redirect:/admin/products/" + g.getProduct().getId() + "#group-" + gid;
    }

    @PostMapping("/groups/{gid}/delete")
    @Transactional
    public String deleteGroup(@PathVariable Long gid, RedirectAttributes flash) {
        OptionGroup g = groups.findById(gid).orElseThrow(NotFoundException::new);
        Product p = g.getProduct();
        p.getOptionGroups().remove(g);
        products.save(p);
        flash.addFlashAttribute("ok", "Removed " + g.getName() + ".");
        return "redirect:/admin/products/" + p.getId();
    }

    // ---- Choices ----

    @PostMapping("/groups/{gid}/choices")
    @Transactional
    public String addChoice(@PathVariable Long gid, @RequestParam String name,
                            @RequestParam(required = false) String priceChange, RedirectAttributes flash) {
        OptionGroup g = groups.findById(gid).orElseThrow(NotFoundException::new);
        if (name == null || name.isBlank()) return back(g.getProduct().getId(), flash, "Name the choice first.");
        OptionChoice c = new OptionChoice();
        c.setGroup(g);
        c.setName(name.trim());
        c.setPriceChange(money(priceChange));
        c.setDisplayOrder(g.getChoices().size());
        c.setDefaultChoice(g.getChoices().isEmpty());
        g.getChoices().add(c);
        groups.save(g);
        flash.addFlashAttribute("ok", "Added " + c.getName() + ".");
        return "redirect:/admin/products/" + g.getProduct().getId() + "#group-" + gid;
    }

    @PostMapping("/choices/{cid}")
    @Transactional
    public String updateChoice(@PathVariable Long cid, @RequestParam String name,
                               @RequestParam(required = false) String priceChange,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) Integer textLimit,
                               @RequestParam(defaultValue = "0") int displayOrder,
                               @RequestParam(defaultValue = "false") boolean halvesPrice,
                               @RequestParam(defaultValue = "false") boolean priceTbd,
                               @RequestParam(defaultValue = "false") boolean needsDesignApproval,
                               @RequestParam(defaultValue = "false") boolean defaultChoice,
                               @RequestParam(defaultValue = "false") boolean active, RedirectAttributes flash) {
        OptionChoice c = choices.findById(cid).orElseThrow(NotFoundException::new);
        if (name != null && !name.isBlank()) c.setName(name.trim());
        c.setPriceChange(money(priceChange));
        c.setDescription(blank(description));
        c.setTextLimit(textLimit != null && textLimit > 0 ? textLimit : null);
        c.setDisplayOrder(displayOrder);
        c.setHalvesPrice(halvesPrice);
        c.setPriceTbd(priceTbd);
        c.setNeedsDesignApproval(needsDesignApproval);
        c.setActive(active);
        if (defaultChoice && !c.getGroup().isMultiple()) {
            c.getGroup().getChoices().forEach(o -> o.setDefaultChoice(false));
        }
        c.setDefaultChoice(defaultChoice);
        choices.save(c);
        flash.addFlashAttribute("ok", "Saved " + c.getName() + ".");
        return "redirect:/admin/products/" + c.getGroup().getProduct().getId() + "#group-" + c.getGroup().getId();
    }

    @PostMapping("/choices/{cid}/delete")
    @Transactional
    public String deleteChoice(@PathVariable Long cid, RedirectAttributes flash) {
        OptionChoice c = choices.findById(cid).orElseThrow(NotFoundException::new);
        OptionGroup g = c.getGroup();
        g.getChoices().remove(c);
        groups.save(g);
        flash.addFlashAttribute("ok", "Removed " + c.getName() + ".");
        return "redirect:/admin/products/" + g.getProduct().getId() + "#group-" + g.getId();
    }

    // ---- Categories ----

    @PostMapping("/categories")
    @Transactional
    public String addCategory(@RequestParam String name, RedirectAttributes flash) {
        if (name == null || name.isBlank()) return "redirect:/admin/products";
        Category c = new Category();
        c.setName(name.trim());
        String slug = slugify(name);
        String s = slug;
        for (int i = 2; categories.findBySlug(s).isPresent(); i++) s = slug + "-" + i;
        c.setSlug(s);
        c.setDisplayOrder((int) categories.count());
        categories.save(c);
        flash.addFlashAttribute("ok", "Added the " + c.getName() + " category.");
        return "redirect:/admin/products";
    }

    @PostMapping("/categories/{id}")
    @Transactional
    public String updateCategory(@PathVariable Long id, @RequestParam String name,
                                 @RequestParam(defaultValue = "0") int displayOrder, RedirectAttributes flash) {
        Category c = categories.findById(id).orElseThrow(NotFoundException::new);
        if (name != null && !name.isBlank()) c.setName(name.trim());
        c.setDisplayOrder(displayOrder);
        categories.save(c);
        flash.addFlashAttribute("ok", "Saved.");
        return "redirect:/admin/products";
    }

    @PostMapping("/categories/{id}/delete")
    @Transactional
    public String deleteCategory(@PathVariable Long id, RedirectAttributes flash) {
        Category c = categories.findById(id).orElseThrow(NotFoundException::new);
        boolean used = products.findAll().stream().anyMatch(p -> p.getCategory().getId().equals(id));
        if (used) {
            flash.addFlashAttribute("errors", List.of("Move or delete the products in " + c.getName() + " first."));
        } else {
            categories.delete(c);
            flash.addFlashAttribute("ok", "Deleted " + c.getName() + ".");
        }
        return "redirect:/admin/products";
    }

    // ---- helpers ----

    private Product find(Long id) {
        return products.findById(id).orElseThrow(NotFoundException::new);
    }

    private String back(Long productId, RedirectAttributes flash, String error) {
        flash.addFlashAttribute("errors", List.of(error));
        return "redirect:/admin/products/" + productId;
    }

    private String uniqueSlug(String name) {
        String base = slugify(name);
        String s = base;
        for (int i = 2; products.existsBySlug(s); i++) s = base + "-" + i;
        return s;
    }

    static String slugify(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        n = n.toLowerCase(Locale.ROOT).replace("'", "").replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return n.isEmpty() ? "item" : n;
    }

    private static BigDecimal money(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(s.trim().replace("$", "").replace("+", ""));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
