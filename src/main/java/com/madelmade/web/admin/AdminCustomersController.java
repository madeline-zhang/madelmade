package com.madelmade.web.admin;

import com.madelmade.domain.CustomerOrder;
import com.madelmade.domain.OrderStatus;
import com.madelmade.repo.CustomerRepository;
import com.madelmade.repo.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Everyone who has ordered, guests included, grouped by email. */
@Controller
@RequestMapping("/admin/customers")
public class AdminCustomersController {

    public static final class Row {
        public String name;
        public String email;
        public String phone;
        public String contact;
        public boolean hasAccount;
        public int orders;
        public BigDecimal spent = BigDecimal.ZERO;
        public Instant lastOrder;
        public Long lastOrderId;
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getContact() { return contact; }
        public boolean isHasAccount() { return hasAccount; }
        public int getOrders() { return orders; }
        public BigDecimal getSpent() { return spent; }
        public Instant getLastOrder() { return lastOrder; }
        public Long getLastOrderId() { return lastOrderId; }
    }

    private final OrderRepository orders;
    private final CustomerRepository customers;

    public AdminCustomersController(OrderRepository orders, CustomerRepository customers) {
        this.orders = orders;
        this.customers = customers;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        Map<String, Row> rows = new LinkedHashMap<>();
        for (CustomerOrder o : orders.findAllByOrderByCreatedAtDesc()) {
            String key = o.getContactEmail().toLowerCase(Locale.ROOT);
            Row r = rows.computeIfAbsent(key, k -> {
                Row n = new Row();
                n.email = k;
                n.name = o.getContactName();
                n.phone = o.getContactPhone();
                n.contact = o.getContactPref().getLabel();
                n.lastOrder = o.getCreatedAt();
                n.lastOrderId = o.getId();
                return n;
            });
            r.orders++;
            if (o.getStatus() == OrderStatus.COMPLETED && o.getTotal() != null) r.spent = r.spent.add(o.getTotal());
        }
        customers.findAllByOrderByCreatedAtDesc().forEach(c -> {
            Row r = rows.computeIfAbsent(c.getEmail().toLowerCase(Locale.ROOT), k -> {
                Row n = new Row();
                n.email = k;
                n.name = c.getName();
                n.phone = c.getPhone();
                n.contact = c.getContactPref().getLabel();
                return n;
            });
            if (c.getLastSignIn() != null || c.getGoogleId() != null) r.hasAccount = true;
            if (r.name == null || r.name.isBlank()) r.name = c.getName();
        });
        List<Row> list = new ArrayList<>(rows.values());
        if (q != null && !q.isBlank()) {
            String needle = q.toLowerCase(Locale.ROOT).trim();
            list.removeIf(r -> !((r.name != null && r.name.toLowerCase(Locale.ROOT).contains(needle))
                    || r.email.contains(needle) || (r.phone != null && r.phone.contains(needle))));
        }
        list.sort(Comparator.comparing((Row r) -> r.lastOrder, Comparator.nullsLast(Comparator.reverseOrder())));
        model.addAttribute("rows", list);
        model.addAttribute("q", q == null ? "" : q);
        return "admin/customers";
    }
}
