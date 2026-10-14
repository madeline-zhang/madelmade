package com.madelmade.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Audit trail of status changes. */
@Entity
@Table(name = "order_event")
public class OrderEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private CustomerOrder order;
    @Enumerated(EnumType.STRING)
    private OrderStatus oldStatus;
    @Enumerated(EnumType.STRING)
    private OrderStatus newStatus;
    private String note;
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }
    public OrderStatus getOldStatus() { return oldStatus; }
    public void setOldStatus(OrderStatus oldStatus) { this.oldStatus = oldStatus; }
    public OrderStatus getNewStatus() { return newStatus; }
    public void setNewStatus(OrderStatus newStatus) { this.newStatus = newStatus; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Instant getCreatedAt() { return createdAt; }
}
