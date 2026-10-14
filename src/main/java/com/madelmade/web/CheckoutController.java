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
import com.madelmade.service.SettingsService;
import com.madelmade.service.UserFacingException;
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

    public CheckoutController(Cart cart, OrderService orderService, OrderRepository orders, CustomerRepository customers,
                              SettingsService settings, Formats formats) {
        this.cart = cart;
        this.orderService = orderService;
        this.orders = orders;
        this.customers = customers;
        this.settings = settings;
        this.formats = formats;
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
            return show(model, form, e.getMessages());
        }
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
        model.addAttribute("minStandard", minStandard.toString());
        model.addAttribute("minStandardText", formats.longDay(minStandard));
        model.addAttribute("minPriority", minPriority.toString());
        model.addAttribute("priorityHelps", orderService.priorityHelps(cart));
        model.addAttribute("contactPrefs", ContactPref.values());
        List<LocalDate> off = settings.daysOff().stream().filter(d -> !d.isBefore(formats.today())).limit(12).toList();
        model.addAttribute("daysOff", off);
        model.addAttribute("daysOffText", off.stream().map(formats::day).toList());
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
