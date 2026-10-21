package com.madelmade.web.admin;

import com.madelmade.domain.Feedback;
import com.madelmade.repo.FeedbackRepository;
import com.madelmade.web.NotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Notes and bug reports sent from the feedback button, newest open ones first. */
@Controller
@RequestMapping("/admin/feedback")
public class AdminFeedbackController {
    private final FeedbackRepository feedback;

    public AdminFeedbackController(FeedbackRepository feedback) {
        this.feedback = feedback;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", feedback.findAllByOrderByResolvedAscCreatedAtDesc());
        return "admin/feedback";
    }

    @PostMapping("/{id}/resolve")
    @Transactional
    public String resolve(@PathVariable Long id, RedirectAttributes flash) {
        Feedback f = feedback.findById(id).orElseThrow(NotFoundException::new);
        f.setResolved(!f.isResolved());
        flash.addFlashAttribute("ok", f.isResolved() ? "Marked done." : "Moved back to open.");
        return "redirect:/admin/feedback";
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        feedback.deleteById(id);
        flash.addFlashAttribute("ok", "Deleted.");
        return "redirect:/admin/feedback";
    }
}
