package com.madelmade.web.admin;

import com.madelmade.domain.PromoCode;
import com.madelmade.repo.PromoCodeRepository;
import com.madelmade.service.PromoService;
import com.madelmade.web.NotFoundException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Promo codes for friends and family: a percentage off, or free. */
@Controller
@RequestMapping("/admin/promos")
public class AdminPromosController {
    private final PromoCodeRepository codes;

    public AdminPromosController(PromoCodeRepository codes) {
        this.codes = codes;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("codes", codes.findAllByOrderByActiveDescCreatedAtDesc());
        return "admin/promos";
    }

    /** Creates a code (no id) or saves changes to one. */
    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id, @RequestParam String code,
                       @RequestParam String kind, @RequestParam(required = false) String percentOff,
                       @RequestParam(required = false) String maxUses, @RequestParam(required = false) String expiresOn,
                       @RequestParam(required = false) String note,
                       @RequestParam(defaultValue = "false") boolean oncePerCustomer, RedirectAttributes flash) {
        PromoCode p = id == null ? new PromoCode() : codes.findById(id).orElseThrow(NotFoundException::new);
        List<String> errors = new ArrayList<>();
        String clean = PromoService.normalize(code);
        if (!clean.matches("[A-Z0-9_-]{3,40}")) errors.add("Codes need 3 to 40 letters or numbers, with no spaces.");
        codes.findByCodeIgnoreCase(clean).filter(other -> !other.getId().equals(p.getId()))
                .ifPresent(other -> errors.add("There's already a code called " + clean + "."));
        int percent = 100;
        if (!"free".equals(kind)) {
            try {
                percent = Integer.parseInt(percentOff == null ? "" : percentOff.trim().replace("%", ""));
            } catch (NumberFormatException e) {
                percent = -1;
            }
            if (percent < 1 || percent > 100) errors.add("Please give a percentage between 1 and 100.");
        }
        Integer uses = null;
        if (maxUses != null && !maxUses.isBlank()) {
            try {
                uses = Integer.parseInt(maxUses.trim());
                if (uses < 1) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                errors.add("Uses should be a whole number, or blank for no limit.");
            }
        }
        LocalDate expires = null;
        if (expiresOn != null && !expiresOn.isBlank()) {
            try {
                expires = LocalDate.parse(expiresOn);
            } catch (DateTimeParseException e) {
                errors.add("Please pick an end date, or leave it blank.");
            }
        }
        if (!errors.isEmpty()) {
            flash.addFlashAttribute("errors", errors);
            return "redirect:/admin/promos";
        }
        p.setCode(clean);
        p.setPercentOff(percent);
        p.setMaxUses(uses);
        p.setExpiresOn(expires);
        p.setNote(note == null || note.isBlank() ? null : note.trim());
        p.setOncePerCustomer(oncePerCustomer);
        codes.save(p);
        flash.addFlashAttribute("ok", id == null ? clean + " is ready to share." : "Saved " + clean + ".");
        return "redirect:/admin/promos";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes flash) {
        PromoCode p = codes.findById(id).orElseThrow(NotFoundException::new);
        p.setActive(!p.isActive());
        codes.save(p);
        flash.addFlashAttribute("ok", p.getCode() + (p.isActive() ? " is on again." : " is turned off."));
        return "redirect:/admin/promos";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        PromoCode p = codes.findById(id).orElseThrow(NotFoundException::new);
        codes.delete(p);
        flash.addFlashAttribute("ok", "Deleted " + p.getCode() + ". Orders that used it keep their discount.");
        return "redirect:/admin/promos";
    }
}
