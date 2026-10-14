package com.madelmade.service;

import com.madelmade.config.AppProperties;
import com.madelmade.domain.Setting;
import com.madelmade.repo.SettingRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Madeline's settings, stored as key/value rows with sensible defaults. */
@Service
public class SettingsService {

    public static final Map<String, String> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put("orders.open", "true");
        DEFAULTS.put("orders.pausedNote", "Madeline is taking a short break from orders. Check back soon!");
        DEFAULTS.put("orders.daysOff", "");
        DEFAULTS.put("rules.noticeDays", "3");
        DEFAULTS.put("rules.maxLoaves", "3");
        DEFAULTS.put("rules.priorityEnabled", "true");
        DEFAULTS.put("rules.priorityNoticeDays", "1");
        DEFAULTS.put("rules.priorityFee", "5");
        DEFAULTS.put("cert.enabled", "true");
        DEFAULTS.put("cert.price", "1");
        DEFAULTS.put("cert.starter", "Manna");
        DEFAULTS.put("admin.emails", "madelmadewithlove@gmail.com");
        DEFAULTS.put("pickup.area", "Brentwood / Nolensville, TN");
        DEFAULTS.put("pickup.address", "");
        DEFAULTS.put("delivery.enabled", "true");
        DEFAULTS.put("notify.email", "madelmadewithlove@gmail.com");
        DEFAULTS.put("notify.newRequest", "true");
        DEFAULTS.put("notify.customerReply", "true");
        DEFAULTS.put("notify.morningSummary", "false");
        DEFAULTS.put("photos.warmth", "6");
        DEFAULTS.put("photos.brightness", "5");
        for (EmailWording w : EmailWording.values()) {
            DEFAULTS.put(w.subjectKey(), w.defaultSubject);
            DEFAULTS.put(w.introKey(), w.defaultIntro);
        }
    }

    private final SettingRepository repo;
    private final AppProperties props;
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private volatile boolean loaded;

    public SettingsService(SettingRepository repo, AppProperties props) {
        this.repo = repo;
        this.props = props;
    }

    private void load() {
        if (loaded) return;
        synchronized (this) {
            if (loaded) return;
            repo.findAll().forEach(s -> cache.put(s.getKey(), s.getValue() == null ? "" : s.getValue()));
            loaded = true;
        }
    }

    public String get(String key) {
        load();
        return cache.getOrDefault(key, DEFAULTS.getOrDefault(key, ""));
    }

    public boolean flag(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public int number(String key) {
        try {
            return Integer.parseInt(get(key).trim());
        } catch (NumberFormatException e) {
            return Integer.parseInt(DEFAULTS.get(key));
        }
    }

    public BigDecimal money(String key) {
        try {
            return new BigDecimal(get(key).trim().replace("$", ""));
        } catch (NumberFormatException e) {
            return new BigDecimal(DEFAULTS.get(key));
        }
    }

    @Transactional
    public void putAll(Map<String, String> values) {
        values.forEach((k, v) -> {
            if (!DEFAULTS.containsKey(k)) return;
            String clean = v == null ? "" : v.trim();
            Setting s = repo.findById(k).orElseGet(() -> new Setting(k, clean));
            s.setValue(clean);
            repo.save(s);
            cache.put(k, clean);
        });
    }

    // Typed views used across the site.

    public boolean ordersOpen() { return flag("orders.open"); }
    public int noticeDays() { return number("rules.noticeDays"); }
    public int maxLoaves() { return number("rules.maxLoaves"); }
    public boolean priorityEnabled() { return flag("rules.priorityEnabled"); }
    public int priorityNoticeDays() { return number("rules.priorityNoticeDays"); }
    public BigDecimal priorityFee() { return money("rules.priorityFee"); }
    public boolean certificatesEnabled() { return flag("cert.enabled"); }
    public BigDecimal certificatePrice() { return money("cert.price"); }
    public String defaultStarter() { return get("cert.starter"); }
    public boolean deliveryEnabled() { return flag("delivery.enabled"); }
    public String pickupArea() { return get("pickup.area"); }
    public String notifyEmail() { return get("notify.email"); }

    /** Admin emails from Settings plus the bootstrap list from the server configuration. */
    public List<String> adminEmails() {
        List<String> out = new ArrayList<>();
        for (String source : new String[] {get("admin.emails"), props.adminEmails()}) {
            if (source == null) continue;
            Arrays.stream(source.split("[,\\s]+"))
                    .map(String::trim).map(String::toLowerCase)
                    .filter(s -> s.contains("@") && !out.contains(s))
                    .forEach(out::add);
        }
        return out;
    }

    /** Days off as single dates; "2026-11-26..2026-11-28" ranges are expanded. */
    public List<LocalDate> daysOff() {
        List<LocalDate> out = new ArrayList<>();
        for (String part : get("orders.daysOff").split("[,\\n]+")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            try {
                if (p.contains("..")) {
                    String[] ends = p.split("\\.\\.");
                    LocalDate a = LocalDate.parse(ends[0].trim());
                    LocalDate b = LocalDate.parse(ends[1].trim());
                    for (LocalDate d = a; !d.isAfter(b) && out.size() < 400; d = d.plusDays(1)) out.add(d);
                } else {
                    out.add(LocalDate.parse(p));
                }
            } catch (RuntimeException ignored) {
                // A badly typed date is skipped rather than breaking checkout.
            }
        }
        return out;
    }

    public List<String> daysOffEntries() {
        return Arrays.stream(get("orders.daysOff").split("[,\\n]+")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public String subject(EmailWording w) { return get(w.subjectKey()); }
    public String intro(EmailWording w) { return get(w.introKey()); }
}
