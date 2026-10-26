package com.madelmade.web;

import com.madelmade.domain.CustomerOrder;
import com.madelmade.domain.OrderItem;
import com.madelmade.domain.OrderStatus;
import com.madelmade.repo.OrderRepository;
import com.madelmade.repo.ProductRepository;
import com.madelmade.service.Formats;
import com.madelmade.service.OrderService;
import com.madelmade.service.UserFacingException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** The customer's private order page, reached by the link in their emails. */
@Controller
public class StatusController {
    private final OrderRepository orders;
    private final OrderService orderService;
    private final ProductRepository products;
    private final Formats formats;

    public StatusController(OrderRepository orders, OrderService orderService, ProductRepository products, Formats formats) {
        this.orders = orders;
        this.orderService = orderService;
        this.products = products;
        this.formats = formats;
    }

    public record Step(String title, String sub, String state) {}

    @GetMapping("/o/{token}")
    public String status(@PathVariable String token, Model model) {
        CustomerOrder o = find(token);
        model.addAttribute("order", o);
        model.addAttribute("steps", steps(o));
        model.addAttribute("headline", headline(o));
        model.addAttribute("subline", subline(o));
        model.addAttribute("certItems", o.getStatus() == OrderStatus.BAKED || o.getStatus() == OrderStatus.COMPLETED
                ? o.getItems().stream().filter(OrderItem::isHasCertificate).toList() : List.of());
        return "shop/status";
    }

    @PostMapping("/o/{token}/reply")
    public String reply(@PathVariable String token, @RequestParam String message, RedirectAttributes flash) {
        CustomerOrder o = find(token);
        try {
            orderService.customerReply(o, message);
            flash.addFlashAttribute("ok", "Sent to Madeline.");
        } catch (UserFacingException e) {
            flash.addFlashAttribute("errors", e.getMessages());
        }
        return "redirect:/o/" + token;
    }

    @PostMapping("/o/{token}/cancel")
    public String cancel(@PathVariable String token, RedirectAttributes flash) {
        CustomerOrder o = find(token);
        try {
            orderService.customerCancel(o);
            flash.addFlashAttribute("ok", "Your order is cancelled. Madeline has been told.");
        } catch (UserFacingException e) {
            flash.addFlashAttribute("errors", e.getMessages());
        }
        return "redirect:/o/" + token;
    }

    @GetMapping("/o/{token}/certificate/{itemId}")
    public String certificate(@PathVariable String token, @PathVariable Long itemId, Model model) {
        CustomerOrder o = find(token);
        OrderItem item = o.getItems().stream().filter(i -> i.getId().equals(itemId) && i.isHasCertificate())
                .findFirst().orElseThrow(NotFoundException::new);
        model.addAttribute("order", o);
        model.addAttribute("item", item);
        model.addAttribute("photoId", item.getProductId() == null ? null
                : products.findById(item.getProductId()).map(p -> p.getPhotoId()).orElse(null));
        return "shop/certificate";
    }

    private CustomerOrder find(String token) {
        return orders.findByStatusToken(token).orElseThrow(NotFoundException::new);
    }

    private String headline(CustomerOrder o) {
        return switch (o.getStatus()) {
            case NEEDS_REVIEW -> "Request sent";
            case CHANGES_REQUESTED -> "Madeline has a question";
            case APPROVED -> "Confirmed!";
            case BAKED -> "Your loaf is baked";
            case COMPLETED -> "Enjoy!";
            case DECLINED -> "Not this time";
            case CANCELLED -> "Cancelled";
        };
    }

    private String subline(CustomerOrder o) {
        return switch (o.getStatus()) {
            case NEEDS_REVIEW -> "Madeline will review your request soon. You'll get an email when she does.";
            case CHANGES_REQUESTED -> "Read her message below and reply here. It goes straight to her.";
            case APPROVED -> "Madeline is planning your bake for " + formats.longDay(o.getRequestedDate()) + ".";
            case BAKED -> o.isShipping() ? "Madeline will ship it and send you the tracking number."
                    : "Madeline will confirm the " + (o.isDelivery() ? "delivery" : "pickup") + " time with you.";
            case COMPLETED -> "Thank you for ordering from MadelMade. We hope it made you smile.";
            case DECLINED -> "Madeline couldn't take this one. Her note is below.";
            case CANCELLED -> "This order was cancelled.";
        };
    }

    private List<Step> steps(CustomerOrder o) {
        List<Step> steps = new ArrayList<>();
        OrderStatus s = o.getStatus();
        int reached = switch (s) {
            case NEEDS_REVIEW, CHANGES_REQUESTED, DECLINED, CANCELLED -> 1;
            case APPROVED -> 2;
            case BAKED -> 3;
            case COMPLETED -> 4;
        };
        steps.add(new Step("Request sent", formats.stamp(o.getCreatedAt()), "done"));
        if (s == OrderStatus.DECLINED || s == OrderStatus.CANCELLED) {
            steps.add(new Step(s.getCustomerLabel(), formats.stamp(o.getUpdatedAt()), "done"));
            return steps;
        }
        steps.add(new Step(s == OrderStatus.CHANGES_REQUESTED ? "Madeline asked a question" : "Madeline confirms",
                o.getApprovedAt() != null ? formats.stamp(o.getApprovedAt()) : "", state(reached, 2)));
        if (o.isDepositRequired()) {
            steps.add(new Step(o.isShipping() ? "Payment" : "Delivery deposit", o.isDepositPaid() ? "Received" : "By Venmo or Zelle after approval",
                    o.isDepositPaid() ? "done" : (reached == 2 ? "current" : "")));
        }
        if (o.isNeedsBaking()) steps.add(new Step("Baked", "Madeline checks in with you", state(reached, 3)));
        steps.add(new Step(o.isShipping() ? "Shipped" : o.isDelivery() ? "Delivered" : "Picked up", formats.longDay(o.getRequestedDate()), state(reached, 4)));
        return steps;
    }

    private static String state(int reached, int step) {
        if (reached >= step) return "done";
        return reached == step - 1 ? "current" : "";
    }
}
