package com.madelmade.service;

import com.madelmade.config.AppProperties;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Sends site email through Brevo. Without a Brevo key (local previews) nothing leaves the
 * server: emails are kept in the outbox page in the dashboard instead.
 */
@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final int OUTBOX_SIZE = 60;

    public record Sent(Instant at, String to, String subject, String html, String result) {}

    private final AppProperties props;
    private final RestClient brevo;
    private final Deque<Sent> outbox = new ArrayDeque<>();

    public MailService(AppProperties props) {
        this.props = props;
        this.brevo = RestClient.builder().baseUrl("https://api.brevo.com/v3").build();
    }

    public boolean isLive() {
        return props.mail().brevoConfigured();
    }

    @Async
    public void send(String to, String toName, String subject, String html) {
        String result;
        if (!isLive()) {
            result = "Not sent: email isn't connected yet";
            log.info("Email (not sent, Brevo not configured) to={} subject={}", to, subject);
        } else {
            try {
                brevo.post().uri("/smtp/email")
                        .header("api-key", props.mail().brevoApiKey())
                        .header("accept", "application/json")
                        .body(Map.of(
                                "sender", Map.of("name", props.mail().fromName(), "email", props.mail().fromEmail()),
                                "to", List.of(toName == null || toName.isBlank()
                                        ? Map.of("email", to) : Map.of("email", to, "name", toName)),
                                "replyTo", Map.of("email", props.mail().replyTo()),
                                "subject", subject,
                                "htmlContent", html))
                        .retrieve()
                        .toBodilessEntity();
                result = "Sent";
            } catch (RuntimeException e) {
                result = "Failed: " + e.getMessage();
                log.warn("Email to {} failed: {}", to, e.getMessage());
            }
        }
        synchronized (outbox) {
            outbox.addFirst(new Sent(Instant.now(), to, subject, html, result));
            while (outbox.size() > OUTBOX_SIZE) outbox.removeLast();
        }
    }

    public List<Sent> outbox() {
        synchronized (outbox) {
            return new ArrayList<>(outbox);
        }
    }
}
