package com.madelmade.service;

import com.madelmade.domain.CustomerOrder;
import com.madelmade.domain.OrderStatus;
import com.madelmade.repo.OrderRepository;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Day-before reminders to customers and the optional morning summary for Madeline, at 7 AM. */
@Component
public class DailyJobs {
    private final OrderRepository orders;
    private final Notifier notifier;
    private final SettingsService settings;
    private final Formats formats;
    private final LoginCodeService codes;

    public DailyJobs(OrderRepository orders, Notifier notifier, SettingsService settings, Formats formats,
                     LoginCodeService codes) {
        this.orders = orders;
        this.notifier = notifier;
        this.settings = settings;
        this.formats = formats;
        this.codes = codes;
    }

    @Scheduled(cron = "0 0 7 * * *", zone = "${app.zone}")
    @Transactional
    public void morning() {
        LocalDate today = formats.today();
        LocalDate tomorrow = today.plusDays(1);
        EnumSet<OrderStatus> active = EnumSet.of(OrderStatus.APPROVED, OrderStatus.BAKED);

        for (CustomerOrder o : orders.findByRequestedDateAndStatusInAndReminderSentFalse(tomorrow, active)) {
            notifier.toCustomer(o, EmailWording.REMINDER, null);
            o.setReminderSent(true);
            orders.save(o);
        }

        if (settings.flag("notify.morningSummary")) {
            List<CustomerOrder> todays = orders.findByRequestedDateBetweenAndStatusInOrderByRequestedDate(today, today, active);
            List<CustomerOrder> tomorrows = orders.findByRequestedDateBetweenAndStatusInOrderByRequestedDate(tomorrow, tomorrow, active);
            if (!todays.isEmpty() || !tomorrows.isEmpty()) notifier.morningSummary(today, todays, tomorrows);
        }
        codes.cleanUp();
    }
}
