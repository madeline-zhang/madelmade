package com.madelmade.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** A note or bug report a visitor sent from the button in the corner of the site. */
@Entity
@Table(name = "feedback")
public class Feedback {
    public static final String FEEDBACK = "feedback";
    public static final String BUG = "bug";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String kind = FEEDBACK;
    @Column(columnDefinition = "text")
    private String message;
    private String email;
    /** The page they were on when they wrote it. */
    private String page;
    private boolean resolved;
    private Instant createdAt = Instant.now();

    public boolean isBug() { return BUG.equals(kind); }

    public Long getId() { return id; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPage() { return page; }
    public void setPage(String page) { this.page = page; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
    public Instant getCreatedAt() { return createdAt; }
}
