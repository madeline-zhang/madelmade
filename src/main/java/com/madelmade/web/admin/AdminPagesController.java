package com.madelmade.web.admin;

import com.madelmade.service.SettingsService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** The words on the About and Gallery pages. The About photo is set through the photo editor. */
@Controller
@RequestMapping("/admin/pages")
public class AdminPagesController {
    static final List<String> KEYS = List.of("about.title", "about.titleAccent", "about.body", "about.faq", "about.signoff",
            "gallery.title", "gallery.titleAccent", "gallery.intro", "gallery.otherTitle", "gallery.otherIntro",
            "notice.pickupOnly", "notice.homeKitchen");

    private final SettingsService settings;

    public AdminPagesController(SettingsService settings) {
        this.settings = settings;
    }

    @GetMapping
    public String page() {
        return "admin/pages";
    }

    @PostMapping
    public String save(@RequestParam MultiValueMap<String, String> form, RedirectAttributes flash) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : KEYS) {
            String v = form.getFirst(key);
            // A cleared box goes back to the starting text rather than leaving a hole on the page.
            if (v != null) values.put(key, v.isBlank() ? SettingsService.DEFAULTS.get(key) : v);
        }
        settings.putAll(values);
        flash.addFlashAttribute("ok", "Pages saved.");
        return "redirect:/admin/pages";
    }
}
