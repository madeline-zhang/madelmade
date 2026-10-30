package com.madelmade.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import com.madelmade.service.TaxCalculator;

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
    /** A tip the customer chose to give, recorded by Madeline. Not part of the price, so never taxed. */
    private BigDecimal tip = BigDecimal.ZERO;
    private String adjustmentNote;
    private String promoCode;
    private Integer promoPercent;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    // Sales tax, with the rates copied in when the order is placed (see applyTaxRates).
    private boolean taxEnabled;
    private boolean taxIncluded;
    private BigDecimal taxRatePrepared = BigDecimal.ZERO;
    private BigDecimal taxRateFood = BigDecimal.ZERO;
    private BigDecimal taxRateLocal = BigDecimal.ZERO;
    private BigDecimal taxablePrepared = BigDecimal.ZERO;
    private BigDecimal taxableFood = BigDecimal.ZERO;
    private BigDecimal nonTaxable = BigDecimal.ZERO;
    private BigDecimal stateTax = BigDecimal.ZERO;
    private BigDecimal localTax = BigDecimal.ZERO;
    private BigDecimal tax = BigDecimal.ZERO;
    private boolean paid;
    private boolean priceTbd;
    private boolean overCapacity;
    private boolean signedIn;
    private String customerNote;
    private String privateNotes;
    private String pickupAddress;
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
        item.setDisplayOrder(items.stream().mapToInt(OrderItem::getDisplayOrder).max().orElse(-1) + 1);
        items.add(item);
    }

    /** Puts a new item where the old one was, so the order's list keeps its order. */
    public void replaceItem(OrderItem old, OrderItem item) {
        int at = items.indexOf(old);
        item.setOrder(this);
        item.setDisplayOrder(old.getDisplayOrder());
        items.set(at, item);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
    }

    /** A note in the order's history that doesn't change its status. */
    public void logChange(String note) {
        changeStatus(status, note);
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
        BigDecimal fee = priorityFee == null ? BigDecimal.ZERO : priorityFee;
        // A promo code takes its percentage off the items and the priority fee, not delivery.
        discount = promoPercent == null ? BigDecimal.ZERO
                : sub.add(fee).multiply(BigDecimal.valueOf(promoPercent))
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal t = sub.add(fee).subtract(discount)
                .add(adjustment == null ? BigDecimal.ZERO : adjustment);
        if (deliveryFee != null) t = t.add(deliveryFee);

        Map<TaxCategory, BigDecimal> byCategory = new EnumMap<>(TaxCategory.class);
        for (OrderItem i : items) byCategory.merge(i.getTaxCategory(), i.getLineTotal(), BigDecimal::add);
        TaxCalculator.Result r = TaxCalculator.compute(getTaxRates(), byCategory, t);
        taxablePrepared = r.taxablePrepared();
        taxableFood = r.taxableFood();
        nonTaxable = r.nonTaxable();
        stateTax = r.stateTax();
        localTax = r.localTax();
        tax = r.tax();
        total = taxEnabled && !taxIncluded ? t.add(tax) : t;
    }

    /** Copies the current tax settings onto the order; later rate changes don't touch it. */
    public void applyTaxRates(TaxCalculator.Rates r) {
        taxEnabled = r.enabled();
        taxIncluded = r.included();
        taxRatePrepared = r.prepared();
        taxRateFood = r.food();
        taxRateLocal = r.local();
    }

    public TaxCalculator.Rates getTaxRates() {
        return new TaxCalculator.Rates(taxEnabled, taxIncluded, taxRatePrepared, taxRateFood, taxRateLocal);
    }

    public String getTaxLabel() {
        return TaxCalculator.label(getTaxRates(), new TaxCalculator.Result(taxablePrepared, taxableFood, nonTaxable, stateTax, localTax));
    }

    /** Total before tax: what was charged for the bread, fees and adjustments. */
    public BigDecimal getPreTaxTotal() {
        return taxEnabled ? total.subtract(tax) : total;
    }

    public String getTaxMode() {
        return !taxEnabled ? "Off" : taxIncluded ? "Included in prices" : "Added at checkout";
    }

    public String getNumber() {
        return "#" + id;
    }

    public boolean isDelivery() {
        return fulfillment == Fulfillment.DELIVERY;
    }

    public boolean isShipping() {
        return fulfillment == Fulfillment.SHIPPING;
    }

    /** Delivered or shipped: the order goes to the customer's address, with a fee Madeline quotes. */
    public boolean isAddressed() {
        return isDelivery() || isShipping();
    }

    /** "Delivery" or "Shipping", for the fee line and the address heading. */
    public String getFeeName() {
        return isShipping() ? "Shipping" : "Delivery";
    }

    public boolean isDepositRequired() {
        return isAddressed() && !depositWaived;
    }

    /** A shipped order is paid in full before it goes out; a delivery only pays its fee up front. */
    public BigDecimal getDepositAmount() {
        return isShipping() ? total : deliveryFee;
    }

    public String getDepositName() {
        return isShipping() ? "payment" : "delivery deposit";
    }

    /** Drinks-only orders (lattes) have nothing to bake, so they skip the Baked step. */
    public boolean isNeedsBaking() {
        return getLoafCount() > 0;
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
    public BigDecimal getTip() { return tip == null ? BigDecimal.ZERO : tip; }
    public void setTip(BigDecimal tip) { this.tip = tip; }
    public BigDecimal getAdjustment() { return adjustment; }
    public void setAdjustment(BigDecimal adjustment) { this.adjustment = adjustment; }
    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }
    public Integer getPromoPercent() { return promoPercent; }
    public void setPromoPercent(Integer promoPercent) { this.promoPercent = promoPercent; }
    public BigDecimal getDiscount() { return discount; }
    public boolean isHasDiscount() { return discount != null && discount.signum() > 0; }
    public String getPromoLabel() {
        if (promoCode == null) return null;
        return promoCode + (promoPercent != null && promoPercent >= 100 ? " (free)" : " (" + promoPercent + "% off)");
    }
    public String getAdjustmentNote() { return adjustmentNote; }
    public void setAdjustmentNote(String adjustmentNote) { this.adjustmentNote = adjustmentNote; }
    public BigDecimal getTotal() { return total; }
    public boolean isTaxEnabled() { return taxEnabled; }
    public boolean isTaxIncluded() { return taxIncluded; }
    public BigDecimal getTaxablePrepared() { return taxablePrepared; }
    public BigDecimal getTaxableFood() { return taxableFood; }
    public BigDecimal getNonTaxable() { return nonTaxable; }
    public BigDecimal getStateTax() { return stateTax; }
    public BigDecimal getLocalTax() { return localTax; }
    public BigDecimal getTax() { return tax; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public boolean isPriceTbd() { return priceTbd; }
    public boolean isOverCapacity() { return overCapacity; }
    public void setOverCapacity(boolean overCapacity) { this.overCapacity = overCapacity; }
    public boolean isSignedIn() { return signedIn; }
    public void setSignedIn(boolean signedIn) { this.signedIn = signedIn; }
    public String getCustomerNote() { return customerNote; }
    public void setCustomerNote(String customerNote) { this.customerNote = customerNote; }
    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }
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
