package com.madelmade.web.admin;

import com.madelmade.domain.OrderStatus;
import com.madelmade.repo.FeedbackRepository;
import com.madelmade.repo.OrderRepository;
import java.util.EnumSet;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackageClasses = AdminModel.class)
public class AdminModel {
    private final OrderRepository orders;
    private final FeedbackRepository feedback;

    public AdminModel(OrderRepository orders, FeedbackRepository feedback) {
        this.orders = orders;
        this.feedback = feedback;
    }

    @ModelAttribute
    public void counts(Model model) {
        model.addAttribute("needsReviewCount",
                orders.countByStatusIn(EnumSet.of(OrderStatus.NEEDS_REVIEW)));
        model.addAttribute("openFeedbackCount", feedback.countByResolvedFalse());
    }
}
