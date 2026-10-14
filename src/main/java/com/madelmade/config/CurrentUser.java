package com.madelmade.config;

import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

/** One way to ask "who is signed in?", whether they used Google or an email code. */
public final class CurrentUser {
    private CurrentUser() {}

    public static Optional<String> email() {
        return email(SecurityContextHolder.getContext().getAuthentication());
    }

    public static Optional<String> email(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        Object p = auth.getPrincipal();
        if (p instanceof AppUser u) return Optional.of(u.email());
        if (p instanceof OidcUser o && o.getEmail() != null) return Optional.of(o.getEmail().toLowerCase());
        if (p instanceof OAuth2User o && o.getAttribute("email") != null) {
            return Optional.of(o.<String>getAttribute("email").toLowerCase());
        }
        return Optional.empty();
    }

    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
