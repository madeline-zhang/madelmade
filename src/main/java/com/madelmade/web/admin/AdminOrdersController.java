package com.madelmade.web.admin;

import com.madelmade.config.AppProperties;
import com.madelmade.domain.CustomerOrder;
import com.madelmade.domain.OrderItem;
import com.madelmade.domain.OrderMessage;
import com.madelmade.domain.OrderStatus;
import com.madelmade.repo.OrderRepository;
import com.madelmade.service.Formats;
import com.madelmade.service.Money;
import com.madelmade.service.OrderService;
import com.madelmade.service.UserFacingException;
import com.madelmade.web.NotFoundException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminOrdersController {
    private static final DateTimeFormatter TODAY = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.US);

    record Tab(String key, String label, Set<OrderStatus> statuses, long count) {}

    private static final Map<String, Set<OrderStatus>> TABS = new java.util.LinkedHashMap<>();
    private static final Map<String, String> LABELS = Map.of("review", "Needs review", "approved", "Approved",
            "baked", "Baked", "completed", "Completed", "declined", "Declined");

    static {
        TABS.put("review", EnumSet.of(OrderStatus.NEEDS_REVIEW, OrderStatus.CHANGES_REQUESTED));
        TABS.put("approved", EnumSet.of(OrderStatus.APPROVED));
        TABS.put("baked", EnumSet.of(OrderStatus.BAKED));
        TABS.put("completed", EnumSet.of(OrderStatus.COMPLETED));
        TABS.put("declined", EnumSet.of(OrderStatus.DECLINED, OrderStatus.CANCELLED));
    }

    private final OrderRepository orders;
    private final OrderService orderService;
    private final Formats formats;
    private final Money money;
    private final AppProperties props;

    public AdminOrdersController(OrderRepository orders, OrderService orderService, Formats formats, Money money,
                                 AppProperties props) {
        this.orders = orders;
        this.orderService = orderService;
        this.formats = formats;
        this.money = money;
        this.props = props;
    }

    @GetMapping({"", "/"})
    public String home() {
        return "redirect:/admin/orders";
    }

    @GetMapping("/orders")
    public String list(@RequestParam(defaultValue = "review") String tab, @RequestParam(required = false) String q,
                       Model model) {
        return render(tab, q, null, model);
    }

    @GetMapping("/orders/{id}")
    public String detail(@PathVariable Long id, @RequestParam(required = false) String tab,
                         @RequestParam(required = false) String q, Model model) {
        CustomerOrder o = find(id);
        String t = tab != null && TABS.containsKey(tab) ? tab : tabFor(o.getStatus());
        return render(t, q, o, model);
    }

    private String render(String tab, String q, CustomerOrder selected, Model model) {
        if (!TABS.containsKey(tab)) tab = "review";
        List<Tab> tabs = new ArrayList<>();
        TABS.forEach((k, v) -> tabs.add(new Tab(k, LABELS.get(k), v, orders.countByStatusIn(v))));
        String query = q == null ? "" : q.trim().toLowerCase();
        List<CustomerOrder> list = orders.findByStatusInOrderByCreatedAtDesc(TABS.get(tab)).stream()
                .filter(o -> query.isEmpty() || matches(o, query))
                .toList();
        if (tab.equals("approved") || tab.equals("baked")) {
            list = list.stream().sorted((a, b) -> a.getRequestedDate().compareTo(b.getRequestedDate())).toList();
        }
        model.addAttribute("tab", tab);
        model.addAttribute("tabs", tabs);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("orders", list);
        model.addAttribute("selected", selected);
        model.addAttribute("todayText", TODAY.format(formats.today()));
        model.addAttribute("inProgress", orderService.loavesInProgress());
        if (selected != null) contactLinks(selected, model);
        return "admin/orders";
    }

    private boolean matches(CustomerOrder o, String q) {
        if (o.getContactName().toLowerCase().contains(q) || o.getContactEmail().toLowerCase().contains(q)) return true;
        String digits = q.replaceAll("\\D", "");
        if (digits.length() >= 3 && o.getContactPhone() != null && o.getContactPhone().replaceAll("\\D", "").contains(digits)) return true;
        if (o.getNumber().contains(q) || String.valueOf(o.getId()).equals(q)) return true;
        if (formats.day(o.getRequestedDate()).toLowerCase().contains(q) || o.getRequestedDate().toString().contains(q)) return true;
        return o.getItems().stream().anyMatch(i -> i.getProductName().toLowerCase().contains(q));
    }

    /** One-tap Text and WhatsApp buttons that open Madeline's phone with a ready message. */
    private void contactLinks(CustomerOrder o, Model model) {
        String digits = o.getContactPhone() == null ? "" : o.getContactPhone().replaceAll("\\D", "");
        if (digits.length() == 10) digits = "1" + digits;
        OrderMessage last = o.getLatestMadelineMessage();
        String body = (last != null ? last.getBody() : "Hi " + o.getCustomer().getFirstName() + ", it's Madeline from MadelMade about your order.")
                + " Your order: " + props.url("/o/" + o.getStatusToken());
        String enc = URLEncoder.encode(body, StandardCharsets.UTF_8).replace("+", "%20");
        model.addAttribute("smsLink", digits.length() >= 10 ? "sms:+" + digits + "?&body=" + enc : null);
        model.addAttribute("waLink", digits.length() >= 10 ? "https://wa.me/" + digits + "?text=" + enc : null);
    }

    private static String tabFor(OrderStatus s) {
        for (var e : TABS.entrySet()) if (e.getValue().contains(s)) return e.getKey();
        return "review";
    }

    // ---- Actions ----

    @PostMapping("/orders/{id}/pricing")
    public String pricing(@PathVariable Long id, @RequestParam MultiValueMap<String, String> form, RedirectAttributes flash) {
        CustomerOrder o = find(id);
        Map<Long, BigDecimal> prices = new HashMap<>();
        for (OrderItem i : o.getItems()) {
            BigDecimal p = decimal(form.getFirst("item-" + i.getId()));
            if (p != null) prices.put(i.getId(), p);
        }
        LocalDate date = null;
        try {
            String d = form.getFirst("requestedDate");
            if (d != null && !d.isBlank()) date = LocalDate.parse(d);
        } catch (RuntimeException ignored) {
            // keep the old date
        }
        final LocalDate newDate = date;
        return act(o, flash, "Saved.", x -> orderService.updatePricing(x, new OrderService.PricingUpdate(
                decimal(form.getFirst("deliveryFee")), decimal(form.getFirst("adjustment")), form.getFirst("adjustmentNote"),
                newDate, form.getFirst("requestedTime"), "true".equals(form.getFirst("depositWaived")),
                "true".equals(form.getFirst("priority")), prices)));
    }

    @PostMapping("/orders/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam(required = false) String message, RedirectAttributes flash) {
        return act(find(id), flash, "Approved. The customer has been emailed.", o -> orderService.approve(o, message));
    }

    @PostMapping("/orders/{id}/changes")
    public String changes(@PathVariable Long id, @RequestParam(required = false) String message, RedirectAttributes flash) {
        return act(find(id), flash, "Sent. The order comes back to Needs review when they reply.", o -> orderService.askForChanges(o, message));
    }

    @PostMapping("/orders/{id}/decline")
    public String decline(@PathVariable Long id, @RequestParam(required = false) String message, RedirectAttributes flash) {
        return act(find(id), flash, "Declined. The customer has been emailed.", o -> orderService.decline(o, message));
    }

    @PostMapping("/orders/{id}/baked")
    public String baked(@PathVariable Long id, @RequestParam MultiValueMap<String, String> form, RedirectAttributes flash) {
        CustomerOrder o = find(id);
        Map<Long, OrderService.CertDetails> certs = new HashMap<>();
        for (OrderItem i : o.getItems()) {
            if (!i.isHasCertificate()) continue;
            certs.put(i.getId(), new OrderService.CertDetails(form.getFirst("certAt-" + i.getId()),
                    form.getFirst("certWeight-" + i.getId()), form.getFirst("certLabor-" + i.getId()),
                    form.getFirst("certStarter-" + i.getId())));
        }
        return act(o, flash, "Marked baked. The customer has been told.", x -> orderService.markBaked(x, certs, form.getFirst("message")));
    }

    @PostMapping("/orders/{id}/completed")
    public String completed(@PathVariable Long id, @RequestParam(required = false) String message, RedirectAttributes flash) {
        return act(find(id), flash, "Completed. Thank-you email sent.", o -> orderService.markCompleted(o, message));
    }

    @PostMapping("/orders/{id}/deposit")
    public String deposit(@PathVariable Long id, @RequestParam boolean paid, RedirectAttributes flash) {
        return act(find(id), flash, paid ? "Deposit marked paid." : "Deposit marked unpaid.", o -> orderService.setDepositPaid(o, paid));
    }

    @PostMapping("/orders/{id}/paid")
    public String paid(@PathVariable Long id, @RequestParam boolean paid, RedirectAttributes flash) {
        return act(find(id), flash, paid ? "Marked paid." : "Marked unpaid.", o -> orderService.setPaid(o, paid));
    }

    @PostMapping("/orders/{id}/message")
    public String message(@PathVariable Long id, @RequestParam(required = false) String message, RedirectAttributes flash) {
        return act(find(id), flash, "Message sent.", o -> orderService.sendMessage(o, message));
    }

    @PostMapping("/orders/{id}/log")
    public String log(@PathVariable Long id, @RequestParam(required = false) String message, RedirectAttributes flash) {
        return act(find(id), flash, "Note logged.", o -> orderService.logNote(o, message));
    }

    @PostMapping("/orders/{id}/notes")
    public String notes(@PathVariable Long id, @RequestParam(required = false) String notes, RedirectAttributes flash) {
        return act(find(id), flash, "Notes saved.", o -> orderService.savePrivateNotes(o, notes));
    }

    @GetMapping("/orders/export.csv")
    public ResponseEntity<String> export() {
        StringBuilder sb = new StringBuilder("Order,Created,Status,Name,Email,Phone,Fulfillment,Date,Items,Subtotal,Priority fee,Delivery fee,Promo code,Discount,Adjustment,Total,Paid\n");
        for (CustomerOrder o : orders.findAllByOrderByCreatedAtDesc()) {
            sb.append(String.join(",", List.of(
                    o.getNumber(), formats.stamp(o.getCreatedAt()), o.getStatus().getLabel(), o.getContactName(),
                    o.getContactEmail(), o.getContactPhone() == null ? "" : o.getContactPhone(), o.getFulfillment().getLabel(),
                    o.getRequestedDate().toString(), o.getItemsSummary(), money.plain(o.getSubtotal()), money.plain(o.getPriorityFee()),
                    o.getDeliveryFee() == null ? "" : money.plain(o.getDeliveryFee()),
                    o.getPromoCode() == null ? "" : o.getPromoCode(), money.plain(o.getDiscount()), money.plain(o.getAdjustment()),
                    money.plain(o.getTotal()), o.isPaid() ? "yes" : "no").stream().map(AdminOrdersController::csv).toList()));
            sb.append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"madelmade-orders-" + formats.today() + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(sb.toString());
    }

    private String act(CustomerOrder o, RedirectAttributes flash, String ok, Consumer<CustomerOrder> action) {
        try {
            action.accept(o);
            flash.addFlashAttribute("ok", ok);
        } catch (UserFacingException e) {
            flash.addFlashAttribute("errors", e.getMessages());
        }
        return "redirect:/admin/orders/" + o.getId();
    }

    private CustomerOrder find(Long id) {
        return orders.findById(id).orElseThrow(NotFoundException::new);
    }

    private static BigDecimal decimal(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s.trim().replace("$", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Quotes a CSV cell and defuses spreadsheet formulas. */
    static String csv(String v) {
        String s = v == null ? "" : v;
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0 && !s.matches("-?\\d+(\\.\\d+)?")) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
