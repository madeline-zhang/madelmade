package com.madelmade.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** One order request: a whole cart, approved or declined as a unit. */
@Entity
@Table(name = "orders")
public class CustomerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
    @SequenceGenerator(name = "orders_seq", sequenceName = "orders_seq", allocationSize = 1)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Customer customer;
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.NEEDS_REVIEW;
    @Enumerated(EnumType.STRING)
    private Fulfillment fulfillment = Fulfillment.PICKUP;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    @Enumerated(EnumType.STRING)
    private ContactPref contactPref = ContactPref.EMAIL;
    private String deliveryAddress;
    private LocalDate requestedDate;
    private String requestedTime;
    private boolean priority;
    private BigDecimal priorityFee = BigDecimal.ZERO;
    /** Quoted by Madeline before approving; null until then. */
    private BigDecimal deliveryFee;
    private boolean depositWaived;
    private boolean depositPaid;
    private BigDecimal subtotal = BigDecimal.ZERO;
    /** Custom price or discount Madeline adds before approving. */
    private BigDecimal adjustment = BigDecimal.ZERO;
    private String adjustmentNote;
    private BigDecimal total = BigDecimal.ZERO;
    private boolean paid;
    private boolean priceTbd;
    private boolean overCapacity;
    private boolean signedIn;
    private String customerNote;
    private String privateNotes;
    private String statusToken;
    private boolean reminderSent;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private Instant approvedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder, id")
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt, id")
    private List<OrderMessage> messages = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt, id")
    private List<OrderEvent> events = new ArrayList<>();

    public void addItem(OrderItem item) {
        item.setOrder(this);
        item.setDisplayOrder(items.size());
        items.add(item);
    }

    public OrderMessage addMessage(MessageAuthor author, String body) {
        OrderMessage m = new OrderMessage();
        m.setOrder(this);
        m.setAuthor(author);
        m.setBody(body);
        messages.add(m);
        return m;
    }

    public void changeStatus(OrderStatus next, String note) {
        OrderEvent e = new OrderEvent();
        e.setOrder(this);
        e.setOldStatus(status);
        e.setNewStatus(next);
        e.setNote(note);
        events.add(e);
        this.status = next;
        this.updatedAt = Instant.now();
    }

    /** Recomputes subtotal and total from the items and fees. */
    public void recalculate() {
        BigDecimal sub = BigDecimal.ZERO;
        boolean tbd = false;
        for (OrderItem i : items) {
            sub = sub.add(i.getLineTotal());
            tbd |= i.isPriceTbd();
        }
        subtotal = sub;
        priceTbd = tbd;
        BigDecimal t = sub.add(priorityFee == null ? BigDecimal.ZERO : priorityFee)
                .add(adjustment == null ? BigDecimal.ZERO : adjustment);
        if (deliveryFee != null) t = t.add(deliveryFee);
        total = t;
    }

    public String getNumber() {
        return "#" + id;
    }

    public boolean isDelivery() {
        return fulfillment == Fulfillment.DELIVERY;
    }

    public boolean isDepositRequired() {
        return isDelivery() && !depositWaived;
    }

    public int getLoafCount() {
        return items.stream().filter(OrderItem::isCountsAsLoaf).mapToInt(OrderItem::getQuantity).sum();
    }

    public List<OrderMessage> getVisibleMessages() {
        return messages.stream().filter(m -> m.getAuthor() != MessageAuthor.LOG).toList();
    }

    public OrderMessage getLatestMadelineMessage() {
        OrderMessage last = null;
        for (OrderMessage m : messages) if (m.getAuthor() == MessageAuthor.MADELINE) last = m;
        return last;
    }

    public String getItemsSummary() {
        StringBuilder sb = new StringBuilder();
        for (OrderItem i : items) {
            if (!sb.isEmpty()) sb.append(", ");
            if (i.getQuantity() > 1) sb.append(i.getQuantity()).append(" × ");
            sb.append(i.getProductName());
            if (i.isHasCertificate()) sb.append(" with birth certificate");
        }
        return sb.toString();
    }

    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public Fulfillment getFulfillment() { return fulfillment; }
    public void setFulfillment(Fulfillment fulfillment) { this.fulfillment = fulfillment; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public ContactPref getContactPref() { return contactPref; }
    public void setContactPref(ContactPref contactPref) { this.contactPref = contactPref; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public LocalDate getRequestedDate() { return requestedDate; }
    public void setRequestedDate(LocalDate requestedDate) { this.requestedDate = requestedDate; }
    public String getRequestedTime() { return requestedTime; }
    public void setRequestedTime(String requestedTime) { this.requestedTime = requestedTime; }
    public boolean isPriority() { return priority; }
    public void setPriority(boolean priority) { this.priority = priority; }
    public BigDecimal getPriorityFee() { return priorityFee; }
    public void setPriorityFee(BigDecimal priorityFee) { this.priorityFee = priorityFee; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }
    public boolean isDepositWaived() { return depositWaived; }
    public void setDepositWaived(boolean depositWaived) { this.depositWaived = depositWaived; }
    public boolean isDepositPaid() { return depositPaid; }
    public void setDepositPaid(boolean depositPaid) { this.depositPaid = depositPaid; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getAdjustment() { return adjustment; }
    public void setAdjustment(BigDecimal adjustment) { this.adjustment = adjustment; }
    public String getAdjustmentNote() { return adjustmentNote; }
    public void setAdjustmentNote(String adjustmentNote) { this.adjustmentNote = adjustmentNote; }
    public BigDecimal getTotal() { return total; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public boolean isPriceTbd() { return priceTbd; }
    public boolean isOverCapacity() { return overCapacity; }
    public void setOverCapacity(boolean overCapacity) { this.overCapacity = overCapacity; }
    public boolean isSignedIn() { return signedIn; }
    public void setSignedIn(boolean signedIn) { this.signedIn = signedIn; }
    public String getCustomerNote() { return customerNote; }
    public void setCustomerNote(String customerNote) { this.customerNote = customerNote; }
    public String getPrivateNotes() { return privateNotes; }
    public void setPrivateNotes(String privateNotes) { this.privateNotes = privateNotes; }
    public String getStatusToken() { return statusToken; }
    public void setStatusToken(String statusToken) { this.statusToken = statusToken; }
    public boolean isReminderSent() { return reminderSent; }
    public void setReminderSent(boolean reminderSent) { this.reminderSent = reminderSent; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public List<OrderItem> getItems() { return items; }
    public List<OrderMessage> getMessages() { return messages; }
    public List<OrderEvent> getEvents() { return events; }
}
