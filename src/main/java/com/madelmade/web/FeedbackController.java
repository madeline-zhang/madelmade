package com.madelmade.web;

import com.madelmade.config.CurrentUser;
import com.madelmade.config.SafeRedirect;
import com.madelmade.domain.Feedback;
import com.madelmade.repo.FeedbackRepository;
import com.madelmade.service.Notifier;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Receives notes and bug reports from the feedback button on every page. */
@Controller
public class FeedbackController {
    private static final String THANKS = "Thanks! Madeline will see this.";
    private static final String EMPTY = "Please write a message first.";

    private final FeedbackRepository feedback;
    private final Notifier notifier;

    public FeedbackController(FeedbackRepository feedback, Notifier notifier) {
        this.feedback = feedback;
        this.notifier = notifier;
    }

    /** From site.js: the panel stays open and shows the reply. */
    @PostMapping(value = "/feedback", headers = "X-Requested-With=fetch")
    @ResponseBody
    public ResponseEntity<String> sendFromPanel(@RequestParam(defaultValue = Feedback.FEEDBACK) String kind,
                                                @RequestParam(required = false) String message,
                                                @RequestParam(required = false) String page,
                                                @RequestParam(required = false) String website) {
        if (!save(kind, message, page, website)) return ResponseEntity.badRequest().body(EMPTY);
        return ResponseEntity.ok(THANKS);
    }

    /** Without JavaScript the form posts normally and comes back to the same page. */
    @PostMapping("/feedback")
    public String send(@RequestParam(defaultValue = Feedback.FEEDBACK) String kind, @RequestParam(required = false) String message,
                       @RequestParam(required = false) String page,
                       @RequestParam(required = false) String website, RedirectAttributes flash) {
        if (save(kind, message, page, website)) flash.addFlashAttribute("ok", THANKS);
        else flash.addFlashAttribute("errors", List.of(EMPTY));
        return "redirect:" + SafeRedirect.orDefault(page, "/");
    }

    private boolean save(String kind, String message, String page, String website) {
        if (message == null || message.isBlank()) return false;
        // "website" is a hidden field people never see; bots fill it in. Pretend it worked.
        if (website != null && !website.isBlank()) return true;
        Feedback f = new Feedback();
        f.setKind(Feedback.BUG.equals(kind) ? Feedback.BUG : Feedback.FEEDBACK);
        f.setMessage(clip(message.trim(), 4000));
        // Signed-in visitors are known by their email; everyone else shows as a guest.
        f.setEmail(CurrentUser.email().orElse(null));
        // For a bug, the page they were on helps track it down.
        if (f.isBug() && page != null && !page.isBlank()) f.setPage(clip(page, 500));
        feedback.save(f);
        notifier.feedbackToMadeline(f);
        return true;
    }

    private static String clip(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }
}
