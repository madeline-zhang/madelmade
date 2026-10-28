package com.madelmade.web.admin;

import com.madelmade.service.EmailWording;
import com.madelmade.service.SettingsService;
import com.madelmade.service.StorageService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/settings")
public class AdminSettingsController {
    /** On/off settings. An unticked box isn't posted, so these are read as false when missing. */
    static final List<String> FLAGS = List.of("orders.open", "rules.priorityEnabled", "cert.enabled", "delivery.enabled", "shipping.enabled",
            "notify.newRequest", "notify.customerReply", "notify.morningSummary", "tax.enabled", "tax.included");

    private final SettingsService settings;
    private final StorageService storage;

    public AdminSettingsController(SettingsService settings, StorageService storage) {
        this.settings = settings;
        this.storage = storage;
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("wordings", EmailWording.values());
        model.addAttribute("daysOff", settings.daysOffEntries());
        StorageService.Usage u = storage.usage();
        model.addAttribute("storage", u);
        model.addAttribute("storageText", Map.of(
                "db", StorageService.mb(u.databaseBytes()),
                "photos", StorageService.mb(u.photoBytes()),
                "videos", StorageService.mb(u.videoBytes()),
                "unused", StorageService.mb(u.unusedPhotoBytes() + u.unusedVideoBytes())));
        return "admin/settings";
    }

    @PostMapping("/storage/clean")
    public String cleanStorage(RedirectAttributes flash) {
        StorageService.Cleanup c;
        try {
            c = storage.clean();
        } catch (RuntimeException e) {
            flash.addFlashAttribute("errors", List.of("The cleanup didn't finish: " + e.getMessage()));
            return "redirect:/admin/settings#storage";
        }
        String removed = "Removed " + c.photos() + " unused photo" + (c.photos() == 1 ? "" : "s") + " and " + c.videos()
                + " unused video" + (c.videos() == 1 ? "" : "s") + ". ";
        if (c.shrinkError() == null) {
            flash.addFlashAttribute("ok", removed + "The database went from " + StorageService.mb(c.bytesBefore())
                    + " to " + StorageService.mb(c.bytesAfter()) + ".");
        } else {
            flash.addFlashAttribute("ok", removed + "That space is free for new photos, but the disk was too full to hand it back to Railway, "
                    + "so Railway's usage won't drop yet. (Postgres said: " + c.shrinkError() + ")");
        }
        return "redirect:/admin/settings#storage";
    }

    @PostMapping
    public String save(@RequestParam MultiValueMap<String, String> form, RedirectAttributes flash) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : SettingsService.DEFAULTS.keySet()) {
            if (key.equals("orders.daysOff")) continue;
            if (FLAGS.contains(key)) {
                values.put(key, String.valueOf(form.containsKey(key)));
            } else if (form.containsKey(key)) {
                values.put(key, form.getFirst(key));
            }
        }
        for (String key : List.of("rules.noticeDays", "rules.maxLoaves", "rules.capacityDays", "rules.maxPerOrder", "rules.priorityNoticeDays")) {
            if (values.containsKey(key) && !values.get(key).trim().matches("\\d{1,2}")) {
                flash.addFlashAttribute("errors", List.of("Please use a whole number for notice days and the loaf limits."));
                return "redirect:/admin/settings";
            }
        }
        for (String key : List.of("tax.ratePrepared", "tax.rateFood", "tax.rateLocal")) {
            if (values.containsKey(key) && !values.get(key).trim().replace("%", "").matches("\\d{1,2}(\\.\\d{1,3})?")) {
                flash.addFlashAttribute("errors", List.of("Please type tax rates as a percent, like 7 or 2.75."));
                return "redirect:/admin/settings";
            }
            if (values.containsKey(key)) values.put(key, values.get(key).trim().replace("%", ""));
        }
        for (String key : List.of("rules.priorityFee", "cert.price")) {
            if (values.containsKey(key) && !values.get(key).trim().replace("$", "").matches("\\d{1,4}(\\.\\d{1,2})?")) {
                flash.addFlashAttribute("errors", List.of("Please type prices as a number, like 5 or 1.50."));
                return "redirect:/admin/settings";
            }
            if (values.containsKey(key)) values.put(key, values.get(key).trim().replace("$", ""));
        }
        String admins = values.getOrDefault("admin.emails", "");
        if (admins.isBlank() || !admins.contains("@")) {
            flash.addFlashAttribute("errors", List.of("Keep at least one admin email, or you'd lock yourself out."));
            return "redirect:/admin/settings";
        }
        settings.putAll(values);
        flash.addFlashAttribute("ok", "Settings saved.");
        return "redirect:/admin/settings";
    }

    @PostMapping("/daysoff/add")
    public String addDayOff(@RequestParam String from, @RequestParam(required = false) String to,
                            @RequestParam(required = false) String note, RedirectAttributes flash) {
        try {
            LocalDate a = LocalDate.parse(from);
            LocalDate b = to == null || to.isBlank() ? a : LocalDate.parse(to);
            if (b.isBefore(a)) { LocalDate t = a; a = b; b = t; }
            LocalDate first = a, last = b;
            // Adding the same days again just updates the note.
            List<String> entries = new ArrayList<>(settings.daysOffEntries().stream()
                    .filter(d -> !(d.from().equals(first) && d.to().equals(last)))
                    .map(SettingsService.DayOff::raw).toList());
            String why = note == null ? "" : note.replaceAll("[\\r\\n|]+", " ").trim();
            entries.add((a.equals(b) ? a.toString() : a + ".." + b) + (why.isEmpty() ? "" : "|" + why));
            settings.putAll(Map.of("orders.daysOff", String.join("\n", entries)));
            flash.addFlashAttribute("ok", "Day off added. Customers can't pick it at checkout.");
        } catch (DateTimeParseException e) {
            flash.addFlashAttribute("errors", List.of("Please pick a date."));
        }
        return "redirect:/admin/settings#days-off";
    }

    @PostMapping("/daysoff/remove")
    public String removeDayOff(@RequestParam String entry, RedirectAttributes flash) {
        List<String> entries = new ArrayList<>(settings.daysOffEntries().stream().map(SettingsService.DayOff::raw).toList());
        entries.remove(entry);
        settings.putAll(Map.of("orders.daysOff", String.join("\n", entries)));
        flash.addFlashAttribute("ok", "Day off removed.");
        return "redirect:/admin/settings#days-off";
    }
}
