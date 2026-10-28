package com.madelmade.web;

import com.madelmade.config.CurrentUser;
import com.madelmade.domain.ContactPref;
import com.madelmade.domain.Customer;
import com.madelmade.domain.CustomerOrder;
import com.madelmade.domain.Fulfillment;
import com.madelmade.repo.CustomerRepository;
import com.madelmade.repo.OrderRepository;
import com.madelmade.service.Cart;
import com.madelmade.service.CheckoutForm;
import com.madelmade.service.Formats;
import com.madelmade.service.OrderService;
import com.madelmade.service.PromoService;
import com.madelmade.domain.PromoCode;
import com.madelmade.service.SettingsService;
import com.madelmade.service.TaxCalculator;
import com.madelmade.service.UserFacingException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CheckoutController {
    private final Cart cart;
    private final OrderService orderService;
    private final OrderRepository orders;
    private final CustomerRepository customers;
    private final SettingsService settings;
    private final Formats formats;
    private final PromoService promos;

    public CheckoutController(Cart cart, OrderService orderService, OrderRepository orders, CustomerRepository customers,
                              SettingsService settings, Formats formats, PromoService promos) {
        this.cart = cart;
        this.orderService = orderService;
        this.orders = orders;
        this.customers = customers;
        this.settings = settings;
        this.formats = formats;
        this.promos = promos;
    }

    @GetMapping("/checkout/start")
    public String start() {
        if (CurrentUser.email().isPresent()) return "redirect:/checkout";
        return cart.isEmpty() ? "redirect:/" : "shop/checkout-start";
    }

    @GetMapping("/checkout")
    public String form(Model model) {
        if (cart.isEmpty()) return "redirect:/#menu";
        CheckoutForm form = new CheckoutForm();
        Optional<String> email = CurrentUser.email();
        email.flatMap(customers::findByEmailIgnoreCase).ifPresent(c -> prefill(form, c));
        email.ifPresent(form::setEmail);
        form.setDate(orderService.earliestDate(cart, false));
        return show(model, form, List.of());
    }

    @PostMapping("/checkout")
    public String submit(@ModelAttribute("form") CheckoutForm form, Model model) {
        try {
            CustomerOrder o = orderService.submit(form, cart, CurrentUser.email());
            return "redirect:/order/received/" + o.getStatusToken();
        } catch (UserFacingException e) {
            CurrentUser.email().ifPresent(form::setEmail);
            model.addAttribute("fieldErrors", e.getByField());
            return show(model, form, e.getByField().isEmpty() ? e.getMessages() : List.of());
        }
    }

    /** "Apply" next to the promo code: shows the discount without sending the request. */
    @PostMapping("/checkout/promo")
    public String applyPromo(@ModelAttribute("form") CheckoutForm form, Model model) {
        if (cart.isEmpty()) return "redirect:/#menu";
        CurrentUser.email().ifPresent(form::setEmail);
        return show(model, form, List.of());
    }

    @GetMapping("/order/received/{token}")
    public String received(@PathVariable String token, Model model) {
        CustomerOrder o = orders.findByStatusToken(token).orElseThrow(NotFoundException::new);
        model.addAttribute("order", o);
        return "shop/received";
    }

    private String show(Model model, CheckoutForm form, List<String> errors) {
        LocalDate minStandard = orderService.earliestDate(cart, false);
        LocalDate minPriority = orderService.earliestDate(cart, true);
        model.addAttribute("form", form);
        model.addAttribute("formErrors", errors);
        BigDecimal promoOff = BigDecimal.ZERO;
        try {
            PromoCode promo = promos.check(form.getPromoCode(), CurrentUser.email().orElse(form.getEmail()));
            if (promo != null) {
                form.setPromoCode(promo.getCode());
                model.addAttribute("promo", promo);
                model.addAttribute("promoDiscount", PromoService.discountOn(cart.getTotal(), promo.getPercentOff()));
                model.addAttribute("promoTotal", cart.getTotal().subtract(PromoService.discountOn(cart.getTotal(), promo.getPercentOff())));
                promoOff = PromoService.discountOn(cart.getTotal(), promo.getPercentOff());
            }
        } catch (UserFacingException e) {
            model.addAttribute("promoError", String.join(" ", e.getMessages()));
        }
        // Estimated sales tax on the items; tax on priority and delivery fees is added with the fees.
        TaxCalculator.Rates rates = settings.taxRates();
        TaxCalculator.Result tax = orderService.estimateTax(cart, promoOff);
        model.addAttribute("taxRates", rates);
        model.addAttribute("taxEst", tax);
        model.addAttribute("taxLabel", TaxCalculator.label(rates, tax));
        model.addAttribute("checkoutTotal", cart.getTotal().subtract(promoOff).add(rates.enabled() && !rates.included() ? tax.tax() : BigDecimal.ZERO));
        model.addAttribute("minStandard", minStandard.toString());
        model.addAttribute("minStandardText", formats.longDay(minStandard));
        model.addAttribute("minPriority", minPriority.toString());
        model.addAttribute("fullDates", String.join(",", orderService.fullDates(cart.getLoafCount(), formats.today()).stream().map(LocalDate::toString).toList()));
        model.addAttribute("overCapacity", form.getDate() != null && orderService.wouldBeOverCapacity(cart.getLoafCount(), form.getDate()));
        model.addAttribute("priorityHelps", orderService.priorityHelps(cart));
        model.addAttribute("contactPrefs", ContactPref.values());
        List<LocalDate> off = settings.daysOff().stream().filter(d -> !d.isBefore(formats.today())).limit(12).toList();
        model.addAttribute("daysOff", off);
        model.addAttribute("daysOffText", settings.upcomingDayOffRanges(formats.today()).stream()
                .limit(6).map(r -> formats.range(r[0], r[1])).toList());
        return "shop/checkout";
    }

    private static void prefill(CheckoutForm f, Customer c) {
        f.setName(c.getName());
        f.setPhone(c.getPhone());
        f.setContactPref(c.getContactPref());
        f.setAddress(c.getAddress());
        f.setFulfillment(Fulfillment.PICKUP);
    }
}
