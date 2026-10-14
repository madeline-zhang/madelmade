package com.madelmade.web;

import com.madelmade.config.AppProperties;
import com.madelmade.config.AppUser;
import com.madelmade.config.CurrentUser;
import com.madelmade.config.NextUrlSuccessHandler;
import com.madelmade.config.SafeRedirect;
import com.madelmade.domain.Customer;
import com.madelmade.service.AdminAccess;
import com.madelmade.service.CustomerService;
import com.madelmade.service.LoginCodeService;
import com.madelmade.service.Notifier;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Sign in with Google, or with a 6-digit code sent by email. One account for everyone. */
@Controller
public class AuthController {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String S_EMAIL = "signin.email";
    private static final String S_DEV_CODE = "signin.devCode";

    private final LoginCodeService codes;
    private final Notifier notifier;
    private final CustomerService customers;
    private final AdminAccess adminAccess;
    private final AppProperties props;
    private final ObjectProvider<ClientRegistrationRepository> google;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(LoginCodeService codes, Notifier notifier, CustomerService customers, AdminAccess adminAccess,
                          AppProperties props, ObjectProvider<ClientRegistrationRepository> google) {
        this.codes = codes;
        this.notifier = notifier;
        this.customers = customers;
        this.adminAccess = adminAccess;
        this.props = props;
        this.google = google;
    }

    @GetMapping("/signin")
    public String signin(@RequestParam(required = false) String next,
                         @RequestParam(required = false) String denied,
                         HttpSession session, Model model) {
        if (denied == null && CurrentUser.email().isPresent()) {
            return "redirect:" + SafeRedirect.orDefault(next, CurrentUser.isAdmin() ? "/admin" : "/profile");
        }
        String target = SafeRedirect.orDefault(next, "");
        if (!target.isEmpty()) session.setAttribute(NextUrlSuccessHandler.SESSION_KEY, target);
        model.addAttribute("next", target);
        model.addAttribute("denied", denied != null);
        model.addAttribute("email", "");
        return "auth/signin";
    }

    @GetMapping("/signin/google")
    public String google(@RequestParam(required = false) String next, HttpSession session) {
        if (google.getIfAvailable() == null) return "redirect:/signin";
        if (SafeRedirect.isLocal(next)) session.setAttribute(NextUrlSuccessHandler.SESSION_KEY, next);
        return "redirect:/oauth2/authorization/google";
    }

    @PostMapping("/signin/email")
    public String sendCode(@RequestParam String email, @RequestParam(required = false) String next,
                           HttpSession session, Model model) {
        String clean = email == null ? "" : email.trim().toLowerCase();
        String target = SafeRedirect.orDefault(next, "");
        if (!EMAIL.matcher(clean).matches()) {
            model.addAttribute("error", "Please enter a valid email address.");
            model.addAttribute("next", target);
            model.addAttribute("denied", false);
            model.addAttribute("email", clean);
            return "auth/signin";
        }
        Optional<String> code = codes.issue(clean);
        if (code.isEmpty()) {
            model.addAttribute("error", "Too many codes requested for this email. Please try again in an hour.");
            model.addAttribute("next", target);
            model.addAttribute("denied", false);
            model.addAttribute("email", clean);
            return "auth/signin";
        }
        notifier.signInCode(clean, code.get());
        session.setAttribute(S_EMAIL, clean);
        if (!target.isEmpty()) session.setAttribute(NextUrlSuccessHandler.SESSION_KEY, target);
        if (props.mail().showCodesOnScreen()) session.setAttribute(S_DEV_CODE, code.get());
        else session.removeAttribute(S_DEV_CODE);
        return "redirect:/signin/code";
    }

    @GetMapping("/signin/code")
    public String codeForm(HttpSession session, Model model) {
        String email = (String) session.getAttribute(S_EMAIL);
        if (email == null) return "redirect:/signin";
        model.addAttribute("email", email);
        model.addAttribute("next", session.getAttribute(NextUrlSuccessHandler.SESSION_KEY));
        model.addAttribute("devCode", session.getAttribute(S_DEV_CODE));
        return "auth/code";
    }

    @PostMapping("/signin/code")
    public String verify(@RequestParam String code, HttpServletRequest request, HttpServletResponse response, Model model) {
        HttpSession session = request.getSession();
        String email = (String) session.getAttribute(S_EMAIL);
        if (email == null) return "redirect:/signin";
        LoginCodeService.Outcome outcome = codes.verify(email, code);
        if (outcome != LoginCodeService.Outcome.OK) {
            model.addAttribute("email", email);
            model.addAttribute("next", session.getAttribute(NextUrlSuccessHandler.SESSION_KEY));
            model.addAttribute("devCode", session.getAttribute(S_DEV_CODE));
            model.addAttribute("error", switch (outcome) {
                case WRONG -> "That code doesn't match. Please check the email and try again.";
                case EXPIRED -> "That code has expired. Send yourself a new one below.";
                default -> "Too many tries. Send yourself a new code below.";
            });
            return "auth/code";
        }

        Customer c = customers.recordSignIn(email, null, null);
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        boolean admin = adminAccess.isAdmin(email);
        if (admin) authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        var auth = UsernamePasswordAuthenticationToken.authenticated(new AppUser(email, c.getName()), null, authorities);
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);

        String next = (String) session.getAttribute(NextUrlSuccessHandler.SESSION_KEY);
        session.removeAttribute(S_EMAIL);
        session.removeAttribute(S_DEV_CODE);
        session.removeAttribute(NextUrlSuccessHandler.SESSION_KEY);
        request.changeSessionId();
        contextRepository.saveContext(ctx, request, response);
        return "redirect:" + SafeRedirect.orDefault(next, admin ? "/admin" : "/profile");
    }
}
