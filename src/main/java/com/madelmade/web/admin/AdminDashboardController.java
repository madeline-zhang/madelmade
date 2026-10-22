package com.madelmade.web.admin;

import com.madelmade.config.AppProperties;
import com.madelmade.domain.Customer;
import com.madelmade.domain.CustomerOrder;
import com.madelmade.domain.OrderItem;
import com.madelmade.domain.OrderStatus;
import com.madelmade.repo.CustomerRepository;
import com.madelmade.repo.OrderRepository;
import com.madelmade.service.Formats;
import com.madelmade.service.OrderService;
import com.madelmade.service.SettingsService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** The admin landing page: how things are going, what's coming up, and the bake calendar. Viewing only. */
@Controller
@RequestMapping("/admin")
public class AdminDashboardController {
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US);
    private static final DateTimeFormatter SHORT_MONTH = DateTimeFormatter.ofPattern("MMM", Locale.US);
    private static final Set<OrderStatus> SHOWN = EnumSet.of(OrderStatus.NEEDS_REVIEW, OrderStatus.CHANGES_REQUESTED,
            OrderStatus.APPROVED, OrderStatus.BAKED, OrderStatus.COMPLETED);

    public record Day(LocalDate date, boolean inMonth, boolean today, boolean off, List<CustomerOrder> orders, int loaves) {}

    /** One month's bar: pct is its height against the best month. */
    public record Bar(String label, String title, BigDecimal amount, int orders, int pct) {
        /** A month with any sales keeps a sliver of bar so it never looks empty. */
        public int getHeight() {
            return amount.signum() > 0 ? Math.max(pct, 2) : 0;
        }
    }

    public record Seller(String name, int count, int pct) {}

    private final OrderRepository orders;
    private final CustomerRepository customers;
    private final OrderService orderService;
    private final SettingsService settings;
    private final Formats formats;
    private final SalesStats sales;
    private final AppProperties props;

    public AdminDashboardController(OrderRepository orders, CustomerRepository customers, OrderService orderService,
                                    SettingsService settings, Formats formats, SalesStats sales, AppProperties props) {
        this.props = props;
        this.orders = orders;
        this.customers = customers;
        this.orderService = orderService;
        this.settings = settings;
        this.formats = formats;
        this.sales = sales;
    }

    @GetMapping({"", "/"})
    public String dashboard(@RequestParam(required = false) String month, Model model) {
        LocalDate today = formats.today();
        List<SalesStats.Sale> done = sales.completed();
        YearMonth thisMonth = YearMonth.from(today);

        model.addAttribute("waiting", orders.countByStatusIn(EnumSet.of(OrderStatus.NEEDS_REVIEW)));
        model.addAttribute("toBake", orders.countByStatusIn(EnumSet.of(OrderStatus.APPROVED)));
        model.addAttribute("readyForPickup", orders.countByStatusIn(EnumSet.of(OrderStatus.BAKED)));
        model.addAttribute("loavesBooked", orderService.loavesBooked());

        YearSales year = sales.yearSales(done);
        List<SalesStats.Sale> thisYear = done.stream().filter(s -> s.on().getYear() == today.getYear()).toList();
        BigDecimal monthGross = sum(done.stream().filter(s -> YearMonth.from(s.on()).equals(thisMonth)).toList());
        model.addAttribute("yearSales", year);
        model.addAttribute("yearPct", Math.min(100, year.gross().multiply(BigDecimal.valueOf(100))
                .divide(YearSales.TAX, 0, RoundingMode.DOWN).intValue()));
        model.addAttribute("todayText", formats.longDay(today));
        model.addAttribute("monthGross", monthGross);
        model.addAttribute("monthName", thisMonth.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.US));
        model.addAttribute("yearOrders", thisYear.size());
        model.addAttribute("averageOrder", thisYear.isEmpty() ? BigDecimal.ZERO
                : year.gross().divide(BigDecimal.valueOf(thisYear.size()), 2, RoundingMode.HALF_UP));
        model.addAttribute("bars", bars(done, thisMonth));
        model.addAttribute("sellers", sellers(thisYear));

        List<Customer> all = customers.findAll();
        model.addAttribute("customerCount", all.size());
        model.addAttribute("newCustomers", all.stream().filter(c -> c.getCreatedAt() != null
                && YearMonth.from(c.getCreatedAt().atZone(props.zoneId())).equals(thisMonth)).count());

        calendar(month, today, model);
        return "admin/dashboard";
    }

    /** The calendar used to be its own page; old links land on the dashboard's calendar. */
    @GetMapping("/calendar")
    public String calendarPage(@RequestParam(required = false) String month) {
        String m = month != null && month.matches("\\d{4}-\\d{2}") ? "?month=" + month : "";
        return "redirect:/admin" + m + "#calendar";
    }

    private static BigDecimal sum(List<SalesStats.Sale> list) {
        return list.stream().map(s -> s.order().getPreTaxTotal()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Sales for each of the last twelve months, ending with this one. */
    private static List<Bar> bars(List<SalesStats.Sale> done, YearMonth end) {
        Map<YearMonth, List<SalesStats.Sale>> by = new LinkedHashMap<>();
        for (int i = 11; i >= 0; i--) by.put(end.minusMonths(i), new ArrayList<>());
        for (var s : done) {
            var list = by.get(YearMonth.from(s.on()));
            if (list != null) list.add(s);
        }
        BigDecimal max = by.values().stream().map(AdminDashboardController::sum).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        List<Bar> bars = new ArrayList<>();
        by.forEach((ym, list) -> {
            BigDecimal amount = sum(list);
            int pct = max.signum() == 0 ? 0 : amount.multiply(BigDecimal.valueOf(100)).divide(max, 0, RoundingMode.HALF_UP).intValue();
            bars.add(new Bar(SHORT_MONTH.format(ym), MONTH.format(ym), amount, list.size(), pct));
        });
        return bars;
    }

    /** What sold most this year, by number ordered. */
    private static List<Seller> sellers(List<SalesStats.Sale> thisYear) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (var s : thisYear) {
            for (OrderItem i : s.order().getItems()) counts.merge(i.getProductName(), i.getQuantity(), Integer::sum);
        }
        int max = counts.values().stream().max(Integer::compare).orElse(0);
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(e -> new Seller(e.getKey(), e.getValue(), max == 0 ? 0 : Math.round(e.getValue() * 100f / max)))
                .toList();
    }

    private void calendar(String month, LocalDate today, Model model) {
        YearMonth ym;
        try {
            ym = month == null ? YearMonth.from(today) : YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            ym = YearMonth.from(today);
        }
        LocalDate start = ym.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate end = ym.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        List<CustomerOrder> inRange = orders.findByRequestedDateBetweenAndStatusInOrderByRequestedDate(start, end, SHOWN);
        Set<LocalDate> off = new HashSet<>(settings.daysOff());

        List<Day> days = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            LocalDate day = d;
            List<CustomerOrder> onDay = inRange.stream().filter(o -> day.equals(o.getRequestedDate())).toList();
            int loaves = onDay.stream().filter(o -> o.getStatus() != OrderStatus.NEEDS_REVIEW
                    && o.getStatus() != OrderStatus.CHANGES_REQUESTED).mapToInt(CustomerOrder::getLoafCount).sum();
            days.add(new Day(d, YearMonth.from(d).equals(ym), d.equals(today), off.contains(d), onDay, loaves));
        }
        model.addAttribute("monthTitle", MONTH.format(ym));
        model.addAttribute("prev", ym.minusMonths(1).toString());
        model.addAttribute("next", ym.plusMonths(1).toString());
        model.addAttribute("days", days);
        model.addAttribute("upcoming", orders.findByRequestedDateBetweenAndStatusInOrderByRequestedDate(today, today.plusDays(13),
                EnumSet.of(OrderStatus.NEEDS_REVIEW, OrderStatus.CHANGES_REQUESTED, OrderStatus.APPROVED, OrderStatus.BAKED)));
    }
}
