package com.madelmade.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(String baseUrl, String zone, String adminEmails, Mail mail) {

    public record Mail(String brevoApiKey, String fromEmail, String fromName, String replyTo, boolean showCodesOnScreen) {
        public boolean brevoConfigured() {
            return brevoApiKey != null && !brevoApiKey.isBlank();
        }
    }

    public ZoneId zoneId() {
        return ZoneId.of(zone == null ? "America/Chicago" : zone);
    }

    public String url(String path) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + path;
    }
}
