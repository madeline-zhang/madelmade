package com.madelmade.web;

import com.madelmade.config.CurrentUser;
import com.madelmade.repo.ProcessStepRepository;
import com.madelmade.service.Cart;
import com.madelmade.service.OrderService;
import com.madelmade.service.PhotoUrls;
import com.madelmade.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Things every page needs: the cart, who's signed in, settings and the current path. */
@ControllerAdvice
public class GlobalModel {
    private final Cart cart;
    private final SettingsService settings;
    private final ObjectProvider<ClientRegistrationRepository> google;
    private final PhotoUrls photoUrls;
    private final ObjectProvider<OrderService> orders;
    private final ProcessStepRepository steps;

    public GlobalModel(Cart cart, SettingsService settings, ObjectProvider<ClientRegistrationRepository> google, PhotoUrls photoUrls,
                       ObjectProvider<OrderService> orders, ProcessStepRepository steps) {
        this.steps = steps;
        this.orders = orders;
        this.cart = cart;
        this.settings = settings;
        this.google = google;
        this.photoUrls = photoUrls;
    }

    @ModelAttribute
    public void common(Model model, HttpServletRequest request) {
        model.addAttribute("cart", cart);
        model.addAttribute("settings", settings);
        model.addAttribute("photoUrls", photoUrls);
        // The How it's made link shows once the page has a step.
        model.addAttribute("showProcess", steps.count() > 0);
        model.addAttribute("signedIn", CurrentUser.email().isPresent());
        model.addAttribute("userEmail", CurrentUser.email().orElse(null));
        model.addAttribute("isAdmin", CurrentUser.isAdmin());
        model.addAttribute("googleEnabled", google.getIfAvailable() != null);
        String q = request.getQueryString();
        model.addAttribute("path", request.getRequestURI() + (q == null ? "" : "?" + q));
        if (!model.containsAttribute("openCart")) model.addAttribute("openCart", false);
    }
}
