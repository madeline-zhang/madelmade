package com.madelmade.domain;

import java.util.EnumSet;
import java.util.Set;

/** Where an order request is in Madeline's approval flow. */
public enum OrderStatus {
    NEEDS_REVIEW("Needs review", "Waiting for Madeline", "review"),
    CHANGES_REQUESTED("Waiting on customer", "Madeline asked about a change", "changes"),
    APPROVED("Approved", "Approved", "approved"),
    BAKED("Baked", "Baked", "baked"),
    COMPLETED("Completed", "Completed", "completed"),
    DECLINED("Declined", "Declined", "declined"),
    CANCELLED("Cancelled", "Cancelled", "declined");

    /** Statuses whose loaves count against the "in progress" capacity. */
    public static final Set<OrderStatus> IN_PROGRESS = EnumSet.of(APPROVED, BAKED);
    /** Statuses still in front of Madeline or the customer. */
    public static final Set<OrderStatus> OPEN = EnumSet.of(NEEDS_REVIEW, CHANGES_REQUESTED, APPROVED, BAKED);

    private final String label;
    private final String customerLabel;
    private final String badge;

    OrderStatus(String label, String customerLabel, String badge) {
        this.label = label;
        this.customerLabel = customerLabel;
        this.badge = badge;
    }

    public String getLabel() { return label; }
    public String getCustomerLabel() { return customerLabel; }
    public String getBadge() { return badge; }

    public boolean isCancellableByCustomer() {
        return this == NEEDS_REVIEW || this == CHANGES_REQUESTED;
    }

    public boolean isClosed() {
        return this == COMPLETED || this == DECLINED || this == CANCELLED;
    }
}
