package com.madelmade.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/** After Google sign-in, returns to where the person was going (checkout, admin, profile). */
public class NextUrlSuccessHandler implements AuthenticationSuccessHandler {
    public static final String SESSION_KEY = "signin.next";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication auth)
            throws IOException {
        HttpSession session = request.getSession(false);
        String next = session == null ? null : (String) session.getAttribute(SESSION_KEY);
        if (session != null) session.removeAttribute(SESSION_KEY);
        if (next == null || !SafeRedirect.isLocal(next)) {
            next = CurrentUser.isAdmin() || auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())) ? "/admin" : "/profile";
        }
        response.sendRedirect(next);
    }
}
