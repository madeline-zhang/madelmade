package com.madelmade.web.admin;

import com.madelmade.domain.OrderStatus;
import com.madelmade.repo.FeedbackRepository;
import com.madelmade.repo.OrderRepository;
import com.madelmade.service.SettingsService;
import java.time.Instant;
import java.util.EnumSet;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackageClasses = AdminModel.class)
public class AdminModel {
    private final OrderRepository orders;
    private final FeedbackRepository feedback;
    private final SettingsService settings;

    public AdminModel(OrderRepository orders, FeedbackRepository feedback, SettingsService settings) {
        this.orders = orders;
        this.feedback = feedback;
        this.settings = settings;
    }

    /** The menu badge counts only new feedback since Madeline last opened the Feedback page. */
    private Instant feedbackSeenAt() {
        try {
            return Instant.ofEpochMilli(Long.parseLong(settings.get("admin.feedbackSeenAt").trim()));
        } catch (NumberFormatException e) {
            return Instant.EPOCH;
        }
    }

    @ModelAttribute
    public void counts(Model model) {
        model.addAttribute("needsReviewCount",
                orders.countByStatusIn(EnumSet.of(OrderStatus.NEEDS_REVIEW)));
        model.addAttribute("openFeedbackCount", feedback.countByResolvedFalseAndCreatedAtAfter(feedbackSeenAt()));
    }
}
