package com.madelmade.web.admin;

import com.madelmade.config.CurrentUser;
import com.madelmade.domain.Feedback;
import com.madelmade.repo.FeedbackRepository;
import com.madelmade.web.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Feedback and bug reports, on separate tabs, with open ones first. Madeline can add her own too. */
@Controller
@RequestMapping("/admin/feedback")
public class AdminFeedbackController {
    private final FeedbackRepository feedback;

    public AdminFeedbackController(FeedbackRepository feedback) {
        this.feedback = feedback;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = Feedback.FEEDBACK) String tab, Model model) {
        String kind = Feedback.BUG.equals(tab) ? Feedback.BUG : Feedback.FEEDBACK;
        model.addAttribute("tab", kind);
        model.addAttribute("items", feedback.findByKindOrderByResolvedAscCreatedAtDesc(kind));
        model.addAttribute("openFeedback", feedback.countByKindAndResolvedFalse(Feedback.FEEDBACK));
        model.addAttribute("openBugs", feedback.countByKindAndResolvedFalse(Feedback.BUG));
        return "admin/feedback";
    }

    /** Madeline's own notes: an idea to remember, or a bug she spotted. */
    @PostMapping("/add")
    @Transactional
    public String add(@RequestParam(defaultValue = Feedback.FEEDBACK) String kind, @RequestParam(required = false) String message,
                      @RequestParam(required = false) String page, RedirectAttributes flash) {
        String k = Feedback.BUG.equals(kind) ? Feedback.BUG : Feedback.FEEDBACK;
        if (message == null || message.isBlank()) {
            flash.addFlashAttribute("errors", List.of("Please write a note first."));
            return "redirect:/admin/feedback?tab=" + k;
        }
        Feedback f = new Feedback();
        f.setKind(k);
        f.setMessage(message.trim().length() > 4000 ? message.trim().substring(0, 4000) : message.trim());
        f.setEmail(CurrentUser.email().orElse(null));
        if (Feedback.BUG.equals(k) && page != null && !page.isBlank()) f.setPage(page.trim().length() > 500 ? page.trim().substring(0, 500) : page.trim());
        feedback.save(f);
        flash.addFlashAttribute("ok", Feedback.BUG.equals(k) ? "Bug added." : "Note added.");
        return "redirect:/admin/feedback?tab=" + k;
    }

    @PostMapping("/{id}/resolve")
    @Transactional
    public String resolve(@PathVariable Long id, RedirectAttributes flash) {
        Feedback f = feedback.findById(id).orElseThrow(NotFoundException::new);
        f.setResolved(!f.isResolved());
        flash.addFlashAttribute("ok", f.isResolved() ? "Marked done." : "Moved back to open.");
        return "redirect:/admin/feedback?tab=" + f.getKind();
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        Feedback f = feedback.findById(id).orElseThrow(NotFoundException::new);
        feedback.delete(f);
        flash.addFlashAttribute("ok", "Deleted.");
        return "redirect:/admin/feedback?tab=" + f.getKind();
    }
}
