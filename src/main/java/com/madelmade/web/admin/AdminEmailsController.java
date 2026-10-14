package com.madelmade.web.admin;

import com.madelmade.service.MailService;
import com.madelmade.web.NotFoundException;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Recent site emails, so Madeline can see exactly what customers received (or would have). */
@Controller
public class AdminEmailsController {
    private final MailService mail;

    public AdminEmailsController(MailService mail) {
        this.mail = mail;
    }

    @GetMapping("/admin/emails")
    public String list(Model model) {
        model.addAttribute("emails", mail.outbox());
        model.addAttribute("live", mail.isLive());
        return "admin/emails";
    }

    @GetMapping("/admin/emails/{index}")
    public ResponseEntity<String> view(@PathVariable int index) {
        List<MailService.Sent> all = mail.outbox();
        if (index < 0 || index >= all.size()) throw new NotFoundException();
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(all.get(index).html());
    }

    @GetMapping("/admin/styleguide")
    public String styleguide() {
        return "admin/styleguide";
    }
}
