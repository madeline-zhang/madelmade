package com.madelmade.web.admin;

import com.madelmade.domain.CustomerOrder;
import com.madelmade.domain.OrderItem;
import com.madelmade.domain.Product;
import com.madelmade.repo.OrderRepository;
import com.madelmade.repo.ProductRepository;
import com.madelmade.service.OrderService;
import com.madelmade.service.PricingService;
import com.madelmade.service.SettingsService;
import com.madelmade.service.UserFacingException;
import com.madelmade.web.NotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Madeline adds, swaps or removes items on an order, using the same builder customers use. */
@Controller
@RequestMapping("/admin/orders/{id}/items")
public class AdminOrderItemsController {
    /** What the product page needs to act as an editor for one order instead of the cart. */
    public record Editing(String orderNumber, String customerName, String action, String back, String button,
                          List<Product> products, String switchBase, Long currentProductId) {}

    private final OrderRepository orders;
    private final ProductRepository products;
    private final PricingService pricing;
    private final OrderService orderService;
    private final SettingsService settings;

    public AdminOrderItemsController(OrderRepository orders, ProductRepository products, PricingService pricing,
                                     OrderService orderService, SettingsService settings) {
        this.orders = orders;
        this.products = products;
        this.pricing = pricing;
        this.orderService = orderService;
        this.settings = settings;
    }

    @GetMapping("/new")
    public String addForm(@PathVariable Long id, @RequestParam(required = false) Long product, Model model) {
        CustomerOrder o = find(id);
        Product p = product != null ? products.findById(product).orElseThrow(NotFoundException::new) : products.findMenu().get(0);
        String base = "/admin/orders/" + id + "/items";
        return render(o, p, null, base, base + "/new", "Add to order " + o.getNumber(), model);
    }

    @GetMapping("/{itemId}")
    public String editForm(@PathVariable Long id, @PathVariable Long itemId, @RequestParam(required = false) Long product,
                           Model model) {
        CustomerOrder o = find(id);
        OrderItem item = item(o, itemId);
        Long pid = product != null ? product : item.getProductId();
        Product p = products.findById(pid).orElseThrow(NotFoundException::new);
        String base = "/admin/orders/" + id + "/items/" + itemId;
        // Start from what the customer picked.
        return render(o, p, item, base, base, "Save to order " + o.getNumber(), model);
    }

    @PostMapping
    public String add(@PathVariable Long id, @RequestParam Long productId, @RequestParam MultiValueMap<String, String> params,
                      RedirectAttributes flash) {
        CustomerOrder o = find(id);
        return save(o, null, productId, params, flash, "/admin/orders/" + id + "/items/new?product=" + productId);
    }

    @PostMapping("/{itemId}")
    public String change(@PathVariable Long id, @PathVariable Long itemId, @RequestParam Long productId,
                         @RequestParam MultiValueMap<String, String> params, RedirectAttributes flash) {
        CustomerOrder o = find(id);
        OrderItem old = item(o, itemId);
        return save(o, old, productId, params, flash, "/admin/orders/" + id + "/items/" + itemId + "?product=" + productId);
    }

    @PostMapping("/{itemId}/remove")
    public String remove(@PathVariable Long id, @PathVariable Long itemId, RedirectAttributes flash) {
        CustomerOrder o = find(id);
        try {
            orderService.removeItem(o, item(o, itemId));
            flash.addFlashAttribute("ok", "Item removed. The total is updated.");
        } catch (UserFacingException e) {
            flash.addFlashAttribute("errors", e.getMessages());
        }
        return "redirect:/admin/orders/" + id;
    }

    private String save(CustomerOrder o, OrderItem old, Long productId, MultiValueMap<String, String> params,
                        RedirectAttributes flash, String backOnError) {
        Product p = products.findById(productId).orElseThrow(NotFoundException::new);
        List<Long> picked = new ArrayList<>();
        params.forEach((k, values) -> {
            if (!k.startsWith("opt-")) return;
            for (String v : values) {
                try {
                    picked.add(Long.parseLong(v));
                } catch (NumberFormatException ignored) {
                    // not a choice id
                }
            }
        });
        String letters = params.getFirst("letters"), font = params.getFirst("font"), notes = params.getFirst("notes");
        boolean cert = "true".equals(params.getFirst("cert"));
        String certName = params.getFirst("certName"), certMessage = params.getFirst("certMessage");
        int qty = 1;
        try {
            qty = Integer.parseInt(params.getFirst("quantity"));
        } catch (RuntimeException ignored) {
            // keep one
        }
        PricingService.Quote q = pricing.quote(p, new PricingService.Selection(picked, letters, font, notes, cert, certName, certMessage));
        try {
            orderService.saveItem(o, old, p, q, qty, letters, font, notes, cert, certName, certMessage);
        } catch (UserFacingException e) {
            flash.addFlashAttribute("errors", e.getMessages());
            flash.addFlashAttribute("selected", picked);
            flash.addFlashAttribute("form_letters", letters == null ? "" : letters);
            flash.addFlashAttribute("form_font", font == null ? "" : font);
            flash.addFlashAttribute("form_notes", notes == null ? "" : notes);
            flash.addFlashAttribute("form_cert", cert);
            flash.addFlashAttribute("form_certName", certName == null ? "" : certName);
            flash.addFlashAttribute("form_certMessage", certMessage == null ? "" : certMessage);
            flash.addFlashAttribute("form_qty", qty);
            return "redirect:" + backOnError;
        }
        flash.addFlashAttribute("ok", (old == null ? "Added " : "Changed to ") + p.getName() + ". The total is updated; the customer sees it on their order page.");
        return "redirect:/admin/orders/" + o.getId();
    }

    private String render(CustomerOrder o, Product p, OrderItem from, String action, String switchBase, String button, Model model) {
        model.addAttribute("product", p);
        model.addAttribute("certPrice", settings.certificatePrice().toPlainString());
        model.addAttribute("editing", new Editing(o.getNumber(), o.getContactName(), action, "/admin/orders/" + o.getId(), button,
                products.findAllOrdered(), switchBase, p.getId()));
        if (from != null && !from.getProductId().equals(p.getId())) {
            // Switching products: keep what the customer wrote, but options start fresh.
            putIfAbsent(model, "form_notes", nz(from.getCustomNotes()));
            putIfAbsent(model, "form_cert", from.isHasCertificate());
            putIfAbsent(model, "form_certName", nz(from.getCertName()));
            putIfAbsent(model, "form_certMessage", nz(from.getCertMessage()));
            putIfAbsent(model, "form_qty", from.getQuantity());
        } else if (from != null) {
            putIfAbsent(model, "selected", from.getChoiceIds() == null || from.getChoiceIds().isBlank() ? List.of()
                    : Arrays.stream(from.getChoiceIds().split(",")).map(String::trim).map(Long::valueOf).toList());
            putIfAbsent(model, "form_letters", nz(from.getLettersText()));
            putIfAbsent(model, "form_font", nz(from.getLettersFont()));
            putIfAbsent(model, "form_notes", nz(from.getCustomNotes()));
            putIfAbsent(model, "form_cert", from.isHasCertificate());
            putIfAbsent(model, "form_certName", nz(from.getCertName()));
            putIfAbsent(model, "form_certMessage", nz(from.getCertMessage()));
            putIfAbsent(model, "form_qty", from.getQuantity());
        }
        for (String k : new String[] {"form_letters", "form_font", "form_notes", "form_certName", "form_certMessage"}) putIfAbsent(model, k, "");
        putIfAbsent(model, "form_cert", false);
        putIfAbsent(model, "form_qty", 1);
        putIfAbsent(model, "selected", List.of());
        return "shop/product";
    }

    private static void putIfAbsent(Model model, String key, Object value) {
        if (!model.containsAttribute(key)) model.addAttribute(key, value);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private CustomerOrder find(Long id) {
        return orders.findById(id).orElseThrow(NotFoundException::new);
    }

    private static OrderItem item(CustomerOrder o, Long itemId) {
        return o.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst().orElseThrow(NotFoundException::new);
    }
}
