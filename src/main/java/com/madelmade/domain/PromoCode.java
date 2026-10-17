package com.madelmade.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/** A code Madeline gives friends and family for a percentage off, or 100% for free. */
@Entity
@Table(name = "promo_code")
public class PromoCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private int percentOff;
    private boolean active = true;
    private Integer maxUses;
    private int timesUsed;
    private LocalDate expiresOn;
    private String note;
    private boolean oncePerCustomer;
    private Instant createdAt = Instant.now();

    /** Why the code can't be used today, or null when it can. */
    public String problem(LocalDate today) {
        if (!active) return "That promo code is no longer active.";
        if (expiresOn != null && today.isAfter(expiresOn)) return "That promo code has expired.";
        if (maxUses != null && timesUsed >= maxUses) return "That promo code has been used up.";
        return null;
    }

    public boolean isOncePerCustomer() { return oncePerCustomer; }
    public void setOncePerCustomer(boolean oncePerCustomer) { this.oncePerCustomer = oncePerCustomer; }
    public boolean isFree() { return percentOff >= 100; }

    public String getLabel() { return isFree() ? "Free" : percentOff + "% off"; }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public int getPercentOff() { return percentOff; }
    public void setPercentOff(int percentOff) { this.percentOff = percentOff; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Integer getMaxUses() { return maxUses; }
    public void setMaxUses(Integer maxUses) { this.maxUses = maxUses; }
    public int getTimesUsed() { return timesUsed; }
    public void setTimesUsed(int timesUsed) { this.timesUsed = timesUsed; }
    public LocalDate getExpiresOn() { return expiresOn; }
    public void setExpiresOn(LocalDate expiresOn) { this.expiresOn = expiresOn; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Instant getCreatedAt() { return createdAt; }
}
