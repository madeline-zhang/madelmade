package com.madelmade.web;

import com.madelmade.config.SafeRedirect;
import com.madelmade.domain.Product;
import com.madelmade.repo.ProductRepository;
import com.madelmade.service.Cart;
import com.madelmade.service.CartLine;
import com.madelmade.service.OrderService;
import com.madelmade.service.PricingService;
import com.madelmade.service.SettingsService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CartController {
    private final Cart cart;
    private final ProductRepository products;
    private final PricingService pricing;
    private final OrderService orders;
    private final SettingsService settings;

    public CartController(Cart cart, ProductRepository products, PricingService pricing, OrderService orders,
                          SettingsService settings) {
        this.cart = cart;
        this.products = products;
        this.pricing = pricing;
        this.orders = orders;
        this.settings = settings;
    }

    @GetMapping("/cart")
    public String cart() {
        return "shop/cart";
    }

    @PostMapping("/cart/add")
    public String add(@RequestParam Long productId,
                      @RequestParam MultiValueMap<String, String> params,
                      @RequestParam(required = false) String letters,
                      @RequestParam(required = false) String notes,
                      @RequestParam(defaultValue = "false") boolean cert,
                      @RequestParam(required = false) String certName,
                      @RequestParam(required = false) String certMessage,
                      @RequestParam(defaultValue = "1") int quantity,
                      RedirectAttributes flash) {
        Product p = products.findById(productId).filter(Product::isActive).orElseThrow(NotFoundException::new);
        // Each option group posts as "opt-<groupId>" so radio buttons in different groups stay independent.
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
        PricingService.Quote q = pricing.quote(p, new PricingService.Selection(picked, letters, notes, cert, certName, certMessage));
        List<String> errors = new ArrayList<>(q.errors());
        int qty = Math.max(1, Math.min(quantity, 12));
        if (!settings.ordersOpen()) errors.add(settings.get("orders.pausedNote"));
        if (p.isSoldOut()) errors.add(p.getName() + " is sold out right now.");
        if (p.isCountsAsLoaf() && cart.getLoafCount() + qty > settings.maxPerOrder()) {
            errors.add(orders.perOrderLimitMessage(cart.getLoafCount()));
        }
        if (!errors.isEmpty()) {
            flash.addFlashAttribute("errors", errors);
            flash.addFlashAttribute("selected", picked);
            flash.addFlashAttribute("form_letters", letters == null ? "" : letters);
            flash.addFlashAttribute("form_notes", notes == null ? "" : notes);
            flash.addFlashAttribute("form_cert", cert);
            flash.addFlashAttribute("form_certName", certName == null ? "" : certName);
            flash.addFlashAttribute("form_certMessage", certMessage == null ? "" : certMessage);
            return "redirect:/p/" + p.getSlug();
        }
        CartLine line = orders.toCartLine(p, q, qty, letters, notes, cert, certName, certMessage);
        cart.add(line);
        flash.addFlashAttribute("openCart", true);
        return "redirect:/p/" + p.getSlug();
    }

    @PostMapping("/cart/update")
    public String update(@RequestParam String line, @RequestParam int quantity,
                         @RequestParam(required = false) String back, RedirectAttributes flash) {
        cart.find(line).ifPresent(l -> {
            if (quantity <= 0) {
                cart.remove(line);
                return;
            }
            int next = Math.min(quantity, 12);
            int otherLoaves = cart.getLoafCount() - (l.isCountsAsLoaf() ? l.getQuantity() : 0);
            if (l.isCountsAsLoaf() && otherLoaves + next > settings.maxPerOrder()) {
                flash.addFlashAttribute("errors", List.of("You can order up to " + settings.maxPerOrder() + " loaves per order."));
                return;
            }
            l.setQuantity(next);
        });
        flash.addFlashAttribute("openCart", true);
        return "redirect:" + SafeRedirect.orDefault(back, "/cart");
    }

    @PostMapping("/cart/remove")
    public String remove(@RequestParam String line, @RequestParam(required = false) String back, RedirectAttributes flash) {
        cart.remove(line);
        flash.addFlashAttribute("openCart", true);
        return "redirect:" + SafeRedirect.orDefault(back, "/cart");
    }
}
