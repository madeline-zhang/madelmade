package com.madelmade.service;

import com.madelmade.config.AppProperties;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Friendly dates for pages and emails, in Madeline's time zone. Used in templates as @fmt. */
@Component("fmt")
public class Formats {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("MMM d", Locale.US);
    private static final DateTimeFormatter LONG_DAY = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("MMM d 'at' h:mm a", Locale.US);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private final AppProperties props;

    public Formats(AppProperties props) {
        this.props = props;
    }

    public LocalDate today() {
        return LocalDate.now(props.zoneId());
    }

    public String day(LocalDate d) {
        return d == null ? "" : DAY.format(d);
    }

    /** "Dec 20", "Dec 20 – 24" or "Dec 20 – Jan 2". */
    public String range(LocalDate a, LocalDate b) {
        if (a == null) return "";
        if (b == null || b.equals(a)) return SHORT.format(a);
        if (a.getYear() == b.getYear() && a.getMonth() == b.getMonth()) return SHORT.format(a) + " – " + b.getDayOfMonth();
        return SHORT.format(a) + " – " + SHORT.format(b);
    }

    public String longDay(LocalDate d) {
        return d == null ? "" : LONG_DAY.format(d);
    }

    public String stamp(Instant i) {
        return i == null ? "" : STAMP.format(i.atZone(props.zoneId()));
    }

    /** "2h ago", "Yesterday", or a date, like the dashboard mockup. */
    public String ago(Instant i) {
        if (i == null) return "";
        Duration d = Duration.between(i, Instant.now());
        if (d.toMinutes() < 1) return "just now";
        if (d.toMinutes() < 60) return d.toMinutes() + "m ago";
        if (d.toHours() < 24) return d.toHours() + "h ago";
        ZonedDateTime z = i.atZone(props.zoneId());
        if (z.toLocalDate().equals(today().minusDays(1))) return "Yesterday";
        return DAY.format(z);
    }

    public String time(Instant i) {
        return i == null ? "" : TIME.format(i.atZone(props.zoneId()));
    }

    public String iso(LocalDate d) {
        return d == null ? "" : d.toString();
    }
}
