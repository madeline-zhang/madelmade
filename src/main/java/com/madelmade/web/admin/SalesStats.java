package com.madelmade.web.admin;

import com.madelmade.config.AppProperties;
import com.madelmade.domain.CustomerOrder;
import com.madelmade.domain.OrderStatus;
import com.madelmade.repo.OrderRepository;
import com.madelmade.service.Formats;
import com.madelmade.service.SettingsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Component;

/** Completed orders and the day each was picked up or delivered, which is when a sale counts. */
@Component
class SalesStats {
    record Sale(CustomerOrder order, LocalDate on) {}

    private final OrderRepository orders;
    private final AppProperties props;
    private final Formats formats;
    private final SettingsService settings;

    SalesStats(OrderRepository orders, AppProperties props, Formats formats, SettingsService settings) {
        this.orders = orders;
        this.props = props;
        this.formats = formats;
        this.settings = settings;
    }

    List<Sale> completed() {
        return orders.findByStatusInOrderByCreatedAtDesc(EnumSet.of(OrderStatus.COMPLETED)).stream()
                .map(o -> new Sale(o, completedOn(o))).filter(s -> s.on() != null).toList();
    }

    /** Gross sales before tax for orders completed this calendar year; cancelled and declined orders never count. */
    YearSales yearSales(List<Sale> sales) {
        int year = formats.today().getYear();
        BigDecimal gross = sales.stream().filter(s -> s.on().getYear() == year)
                .map(s -> s.order().getPreTaxTotal()).reduce(BigDecimal.ZERO, BigDecimal::add);
        return YearSales.of(year, gross, settings.taxRates().enabled());
    }

    /** The day an order was picked up or delivered, from its history. */
    LocalDate completedOn(CustomerOrder o) {
        LocalDate d = null;
        for (var e : o.getEvents()) {
            if (e.getNewStatus() == OrderStatus.COMPLETED) d = e.getCreatedAt().atZone(props.zoneId()).toLocalDate();
        }
        return o.getStatus() == OrderStatus.COMPLETED ? d : null;
    }
}
