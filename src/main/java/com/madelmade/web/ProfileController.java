package com.madelmade.web;

import com.madelmade.config.CurrentUser;
import com.madelmade.domain.ContactPref;
import com.madelmade.domain.Customer;
import com.madelmade.domain.CustomerOrder;
import com.madelmade.repo.CustomerRepository;
import com.madelmade.repo.OrderRepository;
import com.madelmade.service.Cart;
import com.madelmade.service.CustomerService;
import com.madelmade.service.OrderService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {
    private final CustomerService customerService;
    private final CustomerRepository customers;
    private final OrderRepository orders;
    private final OrderService orderService;
    private final Cart cart;

    public ProfileController(CustomerService customerService, CustomerRepository customers, OrderRepository orders,
                             OrderService orderService, Cart cart) {
        this.customerService = customerService;
        this.customers = customers;
        this.orders = orders;
        this.orderService = orderService;
        this.cart = cart;
    }

    private Customer me() {
        String email = CurrentUser.email().orElseThrow(NotFoundException::new);
        return customerService.findOrCreate(email, null);
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        Customer c = me();
        model.addAttribute("customer", c);
        model.addAttribute("orders", orders.findForEmail(c.getEmail()));
        model.addAttribute("contactPrefs", ContactPref.values());
        return "account/profile";
    }

    @PostMapping("/profile")
    public String save(@RequestParam(required = false) String name, @RequestParam(required = false) String phone,
                       @RequestParam(defaultValue = "EMAIL") ContactPref contactPref,
                       @RequestParam(required = false) String address, RedirectAttributes flash) {
        Customer c = me();
        c.setName(blankToNull(name));
        c.setPhone(blankToNull(phone));
        c.setContactPref(contactPref);
        c.setAddress(blankToNull(address));
        customers.save(c);
        flash.addFlashAttribute("ok", "Saved.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/reorder/{id}")
    public String reorder(@PathVariable Long id, RedirectAttributes flash) {
        Customer c = me();
        CustomerOrder o = orders.findById(id)
                .filter(x -> x.getCustomer().getId().equals(c.getId()) || x.getContactEmail().equalsIgnoreCase(c.getEmail()))
                .orElseThrow(NotFoundException::new);
        List<String> skipped = orderService.reorder(o, cart);
        if (!skipped.isEmpty()) {
            flash.addFlashAttribute("errors", List.of("Not on the menu right now: " + String.join(", ", skipped) + "."));
        } else {
            flash.addFlashAttribute("ok", "Added to your cart. Pick a date at checkout.");
        }
        flash.addFlashAttribute("openCart", true);
        return "redirect:/";
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
