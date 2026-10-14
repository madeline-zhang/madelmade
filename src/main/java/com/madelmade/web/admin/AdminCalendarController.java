package com.madelmade.web.admin;

import com.madelmade.domain.CustomerOrder;
import com.madelmade.domain.OrderStatus;
import com.madelmade.repo.OrderRepository;
import com.madelmade.service.Formats;
import com.madelmade.service.SettingsService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/calendar")
public class AdminCalendarController {
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US);
    private static final Set<OrderStatus> SHOWN = EnumSet.of(OrderStatus.NEEDS_REVIEW, OrderStatus.CHANGES_REQUESTED,
            OrderStatus.APPROVED, OrderStatus.BAKED, OrderStatus.COMPLETED);

    public record Day(LocalDate date, boolean inMonth, boolean today, boolean off, List<CustomerOrder> orders, int loaves) {}

    private final OrderRepository orders;
    private final SettingsService settings;
    private final Formats formats;

    public AdminCalendarController(OrderRepository orders, SettingsService settings, Formats formats) {
        this.orders = orders;
        this.settings = settings;
        this.formats = formats;
    }

    @GetMapping
    public String month(@RequestParam(required = false) String month, Model model) {
        YearMonth ym;
        try {
            ym = month == null ? YearMonth.from(formats.today()) : YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            ym = YearMonth.from(formats.today());
        }
        LocalDate start = ym.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate end = ym.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        List<CustomerOrder> inRange = orders.findByRequestedDateBetweenAndStatusInOrderByRequestedDate(start, end, SHOWN);
        Set<LocalDate> off = new HashSet<>(settings.daysOff());
        LocalDate today = formats.today();

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
        model.addAttribute("upcoming", inRange.stream()
                .filter(o -> !o.getRequestedDate().isBefore(today) && o.getStatus() != OrderStatus.COMPLETED).toList());
        return "admin/calendar";
    }
}
