package com.madelmade.config;

import com.madelmade.service.AdminAccess;
import com.madelmade.service.CustomerService;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    ObjectProvider<ClientRegistrationRepository> google,
                                    OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService) throws Exception {
        http.authorizeHttpRequests(a -> a
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/profile/**").authenticated()
                .anyRequest().permitAll());

        http.exceptionHandling(e -> e.accessDeniedPage("/signin?denied"));
        http.formLogin(f -> f.disable());
        http.httpBasic(b -> b.disable());
        http.logout(l -> l.logoutUrl("/signout").logoutSuccessUrl("/?signedout"));
        http.headers(h -> h
                .contentSecurityPolicy(c -> c.policyDirectives(
                        "default-src 'self'; img-src 'self' data: blob:; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                                + "font-src 'self' https://fonts.gstatic.com; script-src 'self'; frame-ancestors 'none'; form-action 'self' https://accounts.google.com"))
                .referrerPolicy(r -> r.policy(
                        org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));

        // The sign-in page is the entry point for anything that needs an account.
        http.exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
            String target = req.getRequestURI();
            res.sendRedirect("/signin?next=" + java.net.URLEncoder.encode(target, java.nio.charset.StandardCharsets.UTF_8));
        }));

        if (google.getIfAvailable() != null) {
            http.oauth2Login(o -> o
                    .loginPage("/signin")
                    .userInfoEndpoint(u -> u.oidcUserService(oidcUserService))
                    .successHandler(new NextUrlSuccessHandler()));
        }
        return http.build();
    }

    /** Adds the admin role when the Google email is one of Madeline's, and keeps the customer record current. */
    @Bean
    OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(AdminAccess adminAccess, CustomerService customers) {
        OidcUserService delegate = new OidcUserService();
        return request -> {
            OidcUser user = delegate.loadUser(request);
            String email = user.getEmail() == null ? "" : user.getEmail().toLowerCase();
            customers.recordSignIn(email, user.getFullName(), user.getSubject());
            Set<GrantedAuthority> authorities = new HashSet<>(user.getAuthorities());
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            if (adminAccess.isAdmin(email)) authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            return new DefaultOidcUser(authorities, user.getIdToken(), user.getUserInfo(), "email");
        };
    }
}
