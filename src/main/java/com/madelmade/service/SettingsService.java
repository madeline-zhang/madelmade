package com.madelmade.service;

import com.madelmade.config.AppProperties;
import com.madelmade.domain.Setting;
import com.madelmade.repo.SettingRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
        DEFAULTS.put("rules.maxPerOrder", "3");
        DEFAULTS.put("rules.priorityEnabled", "true");
        DEFAULTS.put("rules.priorityNoticeDays", "1");
        DEFAULTS.put("rules.priorityFee", "5");
        DEFAULTS.put("design.fonts", "Block letters\nBubble\nScript\nCustom");
        DEFAULTS.put("cert.enabled", "true");
        DEFAULTS.put("cert.price", "1");
        DEFAULTS.put("cert.starter", "Manna");
        DEFAULTS.put("admin.emails", "madelmadewithlove@gmail.com");
        DEFAULTS.put("pickup.area", "Brentwood / Nolensville, TN");
        DEFAULTS.put("pickup.address", "");
        DEFAULTS.put("producer.name", "Madeline");
        DEFAULTS.put("producer.phone", "");
        DEFAULTS.put("delivery.enabled", "true");
        DEFAULTS.put("notify.email", "madelmadewithlove@gmail.com");
        DEFAULTS.put("notify.newRequest", "true");
        DEFAULTS.put("notify.customerReply", "true");
        DEFAULTS.put("notify.morningSummary", "false");
        DEFAULTS.put("photos.warmth", "20");
        DEFAULTS.put("photos.brightness", "0");
        DEFAULTS.put("photos.contrast", "0");
        DEFAULTS.put("photos.saturation", "0");
        // Page text Madeline edits under Pages.
        DEFAULTS.put("about.title", "It started with");
        DEFAULTS.put("about.titleAccent", "a starter");
        DEFAULTS.put("about.body", "I'm Madeline, a home baker in Brentwood, Tennessee. Someone gave me a sourdough starter, and I didn't want to kill it, so I kept baking.\n\n"
                + "One loaf turned into many, and plain loaves turned into designs: leaves, butterflies, names and birthday messages, each scored by hand.\n\n"
                + "Every loaf is made to order and made to make you smile.");
        DEFAULTS.put("about.photoId", "");
        DEFAULTS.put("about.signoff", "Made with love, Madeline");
        DEFAULTS.put("about.faq", "Do you deliver?\nYes, nearby. Madeline quotes the delivery fee when she approves your order, and it's paid up front as a deposit.\n\n"
                + "How do I pay?\nNothing is charged online. You pay at pickup or delivery with cash, Venmo or Zelle.\n\n"
                + "What about allergens?\nEvery loaf contains wheat. Rosemary garlic asiago contains dairy. Mom's Best is yeast-risen, not sourdough. Ask if you have other allergies.\n\n"
                + "How should I store it?\nTo enjoy it later, slice it and freeze the slices.\n\n"
                + "Best way to reheat?\nToast it and butter it up!");
        DEFAULTS.put("notice.pickupOnly", "Drinks made with milk, like lattes, have to stay cold, so Tennessee's home food law only allows them to be sold in person. That makes lattes pickup only. You can pick one up when you collect your bread, or switch this order to pickup.");
        DEFAULTS.put("notice.homeKitchen", "This product was produced at a private residence that is exempt from state licensing and inspection. This product may contain allergens.");
        DEFAULTS.put("notice.decline", "Madeline has the right to decline any order, for any reason. If she can't take yours, she'll let you know, and you won't be charged anything.");
        // When Madeline last opened Feedback; the menu badge only counts what came in after that.
        DEFAULTS.put("admin.feedbackSeenAt", "0");
        DEFAULTS.put("about.homeTitle", "Made in my home kitchen");
        DEFAULTS.put("about.homeBody", "MadelMade is a home bakery. Everything is made in my kitchen in Tennessee under the state's Food Freedom Act. This product was produced at a private residence that is exempt from state licensing and inspection. This product may contain allergens. Each product page lists its ingredients and allergens. Drinks made with milk are pickup only.");
        DEFAULTS.put("gallery.title", "Loaves that made someone");
        DEFAULTS.put("gallery.titleAccent", "smile");
        DEFAULTS.put("gallery.intro", "Past loaves to spark an idea for yours. See something you love? Mention it in your order.");
        DEFAULTS.put("gallery.otherTitle", "Not just bread");
        DEFAULTS.put("gallery.otherIntro", "A few other things I've baked. If any of these interest you, send me an email and we can talk about it ;)");
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
    /** How many loaves Madeline can have on the go at once, across all orders. */
    public int maxLoaves() { return number("rules.maxLoaves"); }
    /** The most loaves one order can have. */
    public int maxPerOrder() { return number("rules.maxPerOrder"); }
    public boolean priorityEnabled() { return flag("rules.priorityEnabled"); }
    public int priorityNoticeDays() { return number("rules.priorityNoticeDays"); }
    public BigDecimal priorityFee() { return money("rules.priorityFee"); }
    /** Fonts customers pick from for text on a loaf, one per line in Settings. */
    public List<String> fonts() {
        return Arrays.stream(get("design.fonts").split("\\R")).map(String::trim).filter(f -> !f.isEmpty()).distinct().toList();
    }
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

    /** A stretch of days off, with Madeline's private note on why. */
    public record DayOff(LocalDate from, LocalDate to, String note, String raw) {}

    /**
     * Days off are stored one per line as "2026-12-20..2027-01-02|Christmas break".
     * Older entries were comma separated with no note.
     */
    public List<DayOff> daysOffEntries() {
        List<DayOff> out = new ArrayList<>();
        for (String line : get("orders.daysOff").replace("\r", "").split("\n")) {
            List<String> parts = line.contains("|") ? List.of(line) : Arrays.asList(line.split(","));
            for (String part : parts) {
                String raw = part.trim();
                if (raw.isEmpty()) continue;
                int bar = raw.indexOf('|');
                String spec = bar < 0 ? raw : raw.substring(0, bar).trim();
                String note = bar < 0 ? "" : raw.substring(bar + 1).trim();
                try {
                    String[] ends = spec.split("\\.\\.");
                    LocalDate a = LocalDate.parse(ends[0].trim());
                    LocalDate b = ends.length > 1 ? LocalDate.parse(ends[1].trim()) : a;
                    out.add(new DayOff(a, b, note, raw));
                } catch (RuntimeException ignored) {
                    // A badly typed date is skipped rather than breaking checkout.
                }
            }
        }
        out.sort(Comparator.comparing(DayOff::from));
        return out;
    }

    /** Days off as single dates, with ranges expanded. */
    public List<LocalDate> daysOff() {
        List<LocalDate> out = new ArrayList<>();
        for (DayOff d : daysOffEntries()) {
            for (LocalDate x = d.from(); !x.isAfter(d.to()) && out.size() < 400; x = x.plusDays(1)) out.add(x);
        }
        return out;
    }

    /** Upcoming days off merged into ranges, e.g. Dec 20 to Jan 2. Each entry is {first, last}. */
    public List<LocalDate[]> upcomingDayOffRanges(LocalDate today) {
        List<LocalDate> days = daysOff().stream().filter(d -> !d.isBefore(today)).distinct().sorted().toList();
        List<LocalDate[]> out = new ArrayList<>();
        for (LocalDate d : days) {
            LocalDate[] last = out.isEmpty() ? null : out.get(out.size() - 1);
            if (last != null && last[1].plusDays(1).equals(d)) last[1] = d;
            else out.add(new LocalDate[] { d, d });
        }
        return out;
    }

    public record Faq(String question, String answer) {}

    /** FAQ entries typed as blocks: a question line, then its answer, with a blank line between entries. */
    public List<Faq> faqs() {
        List<Faq> out = new ArrayList<>();
        for (String block : get("about.faq").replace("\r", "").split("\n\\s*\n")) {
            String b = block.trim();
            if (b.isEmpty()) continue;
            int nl = b.indexOf('\n');
            out.add(nl < 0 ? new Faq(b, "") : new Faq(b.substring(0, nl).trim(), b.substring(nl + 1).trim()));
        }
        return out;
    }

    /** Text typed with blank lines between paragraphs. */
    public List<String> paragraphs(String key) {
        return Arrays.stream(get(key).replace("\r", "").split("\n\\s*\n")).map(String::trim).filter(p -> !p.isEmpty()).toList();
    }

    public Long aboutPhotoId() {
        try {
            return Long.valueOf(get("about.photoId").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String subject(EmailWording w) { return get(w.subjectKey()); }
    public String intro(EmailWording w) { return get(w.introKey()); }
}
