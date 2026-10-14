package com.madelmade.service;

import com.madelmade.domain.*;
import com.madelmade.repo.CustomerRepository;
import com.madelmade.repo.OrderRepository;
import com.madelmade.repo.ProductRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Order requests and every step of Madeline's approval flow. */
@Service
public class OrderService {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final OrderRepository orders;
    private final ProductRepository products;
    private final CustomerRepository customerRepo;
    private final CustomerService customers;
    private final PricingService pricing;
    private final SettingsService settings;
    private final Notifier notifier;
    private final Formats formats;
    private final SecureRandom random = new SecureRandom();

    public OrderService(OrderRepository orders, ProductRepository products, CustomerRepository customerRepo,
                        CustomerService customers, PricingService pricing, SettingsService settings,
                        Notifier notifier, Formats formats) {
        this.orders = orders;
        this.products = products;
        this.customerRepo = customerRepo;
        this.customers = customers;
        this.pricing = pricing;
        this.settings = settings;
        this.notifier = notifier;
        this.formats = formats;
    }

    // ---- Dates and capacity ----

    public int standardNotice(Cart cart) {
        int notice = cart.isEmpty() ? settings.noticeDays() : 0;
        for (CartLine l : cart.getLines()) {
            Optional<Product> p = products.findById(l.getProductId());
            int lead = p.map(Product::getLeadTimeDays).orElse(null) == null
                    ? settings.noticeDays() : p.get().getLeadTimeDays();
            notice = Math.max(notice, lead);
        }
        return notice;
    }

    public LocalDate earliestDate(Cart cart, boolean priority) {
        int days = priority && settings.priorityEnabled() ? settings.priorityNoticeDays() : standardNotice(cart);
        LocalDate d = formats.today().plusDays(days);
        List<LocalDate> off = settings.daysOff();
        int guard = 0;
        while (off.contains(d) && guard++ < 366) d = d.plusDays(1);
        return d;
    }

    /** Whether priority would actually get the order sooner than standard notice. */
    public boolean priorityHelps(Cart cart) {
        return settings.priorityEnabled() && settings.priorityNoticeDays() < standardNotice(cart);
    }

    public int loavesInProgress() {
        return orders.findByStatusInOrderByCreatedAtDesc(OrderStatus.IN_PROGRESS).stream()
                .mapToInt(CustomerOrder::getLoafCount).sum();
    }

    // ---- Customer side ----

    @Transactional
    public CustomerOrder submit(CheckoutForm f, Cart cart, Optional<String> signedInEmail) {
        List<String> errors = new ArrayList<>();
        if (!settings.ordersOpen()) throw new UserFacingException(settings.get("orders.pausedNote"));
        if (cart.isEmpty()) throw new UserFacingException("Your cart is empty.");
        if (cart.getLoafCount() > settings.maxLoaves()) {
            errors.add("Madeline can bake up to " + settings.maxLoaves() + " loaves per order. Please remove a loaf or two.");
        }

        String email = signedInEmail.orElse(trim(f.getEmail())).toLowerCase();
        if (isBlank(f.getName())) errors.add("Please tell us your name.");
        if (!EMAIL.matcher(email).matches()) errors.add("Please enter a valid email so you get updates.");
        if (f.getContactPref() != ContactPref.EMAIL && isBlank(f.getPhone())) {
            errors.add("Please add a phone number so Madeline can " + (f.getContactPref() == ContactPref.TEXT ? "text" : "WhatsApp") + " you.");
        }
        if (f.getFulfillment() == Fulfillment.DELIVERY) {
            if (!settings.deliveryEnabled()) errors.add("Delivery isn't available right now; please choose pickup.");
            else if (isBlank(f.getAddress())) errors.add("Please enter the delivery address.");
        }
        boolean priority = f.isPriority() && settings.priorityEnabled();
        if (f.getDate() == null) {
            errors.add("Please choose a date.");
        } else {
            LocalDate earliest = earliestDate(cart, priority);
            if (f.getDate().isBefore(earliest)) {
                errors.add("The earliest date for this order is " + formats.longDay(earliest)
                        + (priority || !priorityHelps(cart) ? "." : ", or sooner with priority."));
            } else if (settings.daysOff().contains(f.getDate())) {
                errors.add("Madeline isn't baking on " + formats.longDay(f.getDate()) + ". Please pick another day.");
            } else if (f.getDate().isAfter(formats.today().plusDays(180))) {
                errors.add("Please choose a date within the next six months.");
            }
        }

        // Re-price every line from the current menu so the order is never stale.
        List<OrderItem> items = new ArrayList<>();
        for (CartLine line : cart.getLines()) {
            Optional<Product> p = products.findById(line.getProductId());
            if (p.isEmpty() || !p.get().isActive() || p.get().isSoldOut()) {
                errors.add(line.getProductName() + " is no longer available. Please remove it from your cart.");
                continue;
            }
            PricingService.Quote q = pricing.quote(p.get(), new PricingService.Selection(line.getChoiceIds(),
                    line.getLettersText(), line.getCustomNotes(), line.isCertificate(), line.getCertName(), line.getCertMessage()));
            if (!q.ok()) {
                errors.add(line.getProductName() + ": " + String.join(" ", q.errors()));
                continue;
            }
            items.add(toItem(p.get(), line, q));
        }
        if (!errors.isEmpty()) throw new UserFacingException(errors);

        Customer customer = customers.findOrCreate(email, trim(f.getName()));
        if (isBlank(customer.getName())) customer.setName(trim(f.getName()));
        if (!isBlank(f.getPhone())) customer.setPhone(trim(f.getPhone()));
        customer.setContactPref(f.getContactPref());
        if (f.getFulfillment() == Fulfillment.DELIVERY && !isBlank(f.getAddress())) customer.setAddress(trim(f.getAddress()));
        customerRepo.save(customer);

        CustomerOrder o = new CustomerOrder();
        o.setCustomer(customer);
        o.setSignedIn(signedInEmail.isPresent());
        o.setContactName(trim(f.getName()));
        o.setContactEmail(email);
        o.setContactPhone(trim(f.getPhone()));
        o.setContactPref(f.getContactPref());
        o.setFulfillment(f.getFulfillment());
        o.setDeliveryAddress(f.getFulfillment() == Fulfillment.DELIVERY ? trim(f.getAddress()) : null);
        o.setRequestedDate(f.getDate());
        o.setRequestedTime(trim(f.getTime()));
        o.setPriority(priority);
        o.setPriorityFee(priority ? settings.priorityFee() : BigDecimal.ZERO);
        o.setCustomerNote(trim(f.getNote()));
        o.setStatusToken(newToken());
        items.forEach(o::addItem);
        o.recalculate();
        o.setOverCapacity(loavesInProgress() + o.getLoafCount() > settings.maxLoaves());
        o.changeStatus(OrderStatus.NEEDS_REVIEW, "Request submitted");
        orders.save(o);

        cart.clear();
        notifier.toCustomer(o, EmailWording.RECEIVED, null);
        notifier.newRequestToMadeline(o);
        return o;
    }

    private OrderItem toItem(Product p, CartLine line, PricingService.Quote q) {
        OrderItem i = new OrderItem();
        i.setProductId(p.getId());
        i.setProductName(p.getName());
        i.setQuantity(Math.max(1, line.getQuantity()));
        i.setChoiceIds(q.choiceIds().stream().map(String::valueOf).collect(Collectors.joining(",")));
        i.setOptionsSummary(q.summary());
        i.setCustomNotes(trim(line.getCustomNotes()));
        i.setLettersText(trim(line.getLettersText()));
        i.setUnitPrice(q.unitPrice());
        i.setPriceTbd(q.priceTbd());
        i.setCountsAsLoaf(p.isCountsAsLoaf());
        if (line.isCertificate()) {
            i.setHasCertificate(true);
            i.setCertName(trim(line.getCertName()));
            i.setCertMessage(trim(line.getCertMessage()));
            i.setCertPrice(q.certPrice());
            i.setCertStarter(settings.defaultStarter());
        }
        i.recalculate();
        return i;
    }

    @Transactional
    public void customerReply(CustomerOrder o, String text) {
        if (isBlank(text)) throw new UserFacingException("Please write a message first.");
        if (text.length() > 4000) throw new UserFacingException("That message is a little long. Please shorten it.");
        OrderMessage m = o.addMessage(MessageAuthor.CUSTOMER, text.trim());
        if (o.getStatus() == OrderStatus.CHANGES_REQUESTED) {
            o.changeStatus(OrderStatus.NEEDS_REVIEW, "Customer replied");
        }
        o.setUpdatedAt(Instant.now());
        orders.save(o);
        notifier.customerReplyToMadeline(o, m);
    }

    @Transactional
    public void customerCancel(CustomerOrder o) {
        if (!o.getStatus().isCancellableByCustomer()) {
            throw new UserFacingException("This order can't be cancelled online anymore. Please message Madeline.");
        }
        o.changeStatus(OrderStatus.CANCELLED, "Cancelled by customer");
        orders.save(o);
        notifier.customerCancelledToMadeline(o);
    }

    /** Copies a past order's items back into the cart; returns names that couldn't be added. */
    public List<String> reorder(CustomerOrder o, Cart cart) {
        List<String> skipped = new ArrayList<>();
        for (OrderItem i : o.getItems()) {
            Optional<Product> p = i.getProductId() == null ? Optional.empty() : products.findById(i.getProductId());
            if (p.isEmpty() || !p.get().isActive() || p.get().isSoldOut()) {
                skipped.add(i.getProductName());
                continue;
            }
            List<Long> ids = parseIds(i.getChoiceIds());
            boolean cert = i.isHasCertificate() && settings.certificatesEnabled() && p.get().isAllowsBirthCertificate();
            PricingService.Quote q = pricing.quote(p.get(), new PricingService.Selection(ids, i.getLettersText(),
                    i.getCustomNotes(), cert, i.getCertName(), i.getCertMessage()));
            if (!q.ok()) {
                skipped.add(i.getProductName());
                continue;
            }
            cart.add(toCartLine(p.get(), q, i.getQuantity(), i.getLettersText(), i.getCustomNotes(), cert,
                    i.getCertName(), i.getCertMessage()));
        }
        return skipped;
    }

    public CartLine toCartLine(Product p, PricingService.Quote q, int qty, String letters, String notes,
                               boolean cert, String certName, String certMessage) {
        CartLine l = new CartLine();
        l.setProductId(p.getId());
        l.setProductName(p.getName());
        l.setSlug(p.getSlug());
        l.setPhotoId(p.getPhotoId());
        l.setChoiceIds(new ArrayList<>(q.choiceIds()));
        l.setOptionsSummary(q.summary());
        l.setLettersText(trim(letters));
        l.setCustomNotes(trim(notes));
        l.setCertificate(cert);
        l.setCertName(cert ? trim(certName) : null);
        l.setCertMessage(cert ? trim(certMessage) : null);
        l.setCertPrice(q.certPrice());
        l.setUnitPrice(q.unitPrice());
        l.setQuantity(Math.max(1, Math.min(qty, 6)));
        l.setPriceTbd(q.priceTbd());
        l.setCountsAsLoaf(p.isCountsAsLoaf());
        return l;
    }

    // ---- Madeline's side ----

    public record PricingUpdate(BigDecimal deliveryFee, BigDecimal adjustment, String adjustmentNote,
                                LocalDate requestedDate, String requestedTime, boolean depositWaived,
                                boolean priority, Map<Long, BigDecimal> itemPrices) {}

    @Transactional
    public void updatePricing(CustomerOrder o, PricingUpdate u) {
        if (o.getStatus().isClosed()) throw new UserFacingException("This order is closed.");
        o.setDeliveryFee(o.isDelivery() ? u.deliveryFee() : null);
        o.setAdjustment(u.adjustment() == null ? BigDecimal.ZERO : u.adjustment());
        o.setAdjustmentNote(trim(u.adjustmentNote()));
        if (u.requestedDate() != null) o.setRequestedDate(u.requestedDate());
        o.setRequestedTime(trim(u.requestedTime()));
        o.setDepositWaived(u.depositWaived());
        o.setPriority(u.priority());
        o.setPriorityFee(u.priority() ? (o.getPriorityFee().signum() > 0 ? o.getPriorityFee() : settings.priorityFee()) : BigDecimal.ZERO);
        if (u.itemPrices() != null) {
            for (OrderItem i : o.getItems()) {
                BigDecimal p = u.itemPrices().get(i.getId());
                if (p == null) continue;
                BigDecimal unit = p.add(i.isHasCertificate() ? i.getCertPrice() : BigDecimal.ZERO);
                if (unit.compareTo(i.getUnitPrice()) != 0 || i.isPriceTbd()) {
                    i.setUnitPrice(unit);
                    i.setPriceTbd(false);
                    i.recalculate();
                }
            }
        }
        o.recalculate();
        o.setUpdatedAt(Instant.now());
        orders.save(o);
    }

    @Transactional
    public void approve(CustomerOrder o, String message) {
        requireStatus(o, OrderStatus.NEEDS_REVIEW, OrderStatus.CHANGES_REQUESTED);
        if (o.isDelivery() && o.getDeliveryFee() == null) {
            throw new UserFacingException("Set the delivery fee before approving a delivery order.");
        }
        if (o.isPriceTbd()) {
            throw new UserFacingException("Set a price for the custom items before approving.");
        }
        saveMadelineMessage(o, message);
        o.setApprovedAt(Instant.now());
        o.changeStatus(OrderStatus.APPROVED, "Approved");
        orders.save(o);
        notifier.toCustomer(o, EmailWording.APPROVED, message);
    }

    @Transactional
    public void askForChanges(CustomerOrder o, String message) {
        requireStatus(o, OrderStatus.NEEDS_REVIEW, OrderStatus.CHANGES_REQUESTED, OrderStatus.APPROVED);
        if (isBlank(message)) throw new UserFacingException("Write what you'd like to change so the customer can reply.");
        saveMadelineMessage(o, message);
        o.changeStatus(OrderStatus.CHANGES_REQUESTED, "Asked for changes");
        orders.save(o);
        notifier.toCustomer(o, EmailWording.CHANGES, message);
    }

    @Transactional
    public void decline(CustomerOrder o, String message) {
        requireStatus(o, OrderStatus.NEEDS_REVIEW, OrderStatus.CHANGES_REQUESTED, OrderStatus.APPROVED);
        saveMadelineMessage(o, message);
        o.changeStatus(OrderStatus.DECLINED, "Declined");
        orders.save(o);
        notifier.toCustomer(o, EmailWording.DECLINED, message);
    }

    public record CertDetails(String bakedAt, String weight, String labor, String starter) {}

    @Transactional
    public void markBaked(CustomerOrder o, Map<Long, CertDetails> certs, String message) {
        requireStatus(o, OrderStatus.APPROVED);
        for (OrderItem i : o.getItems()) {
            CertDetails c = certs.get(i.getId());
            if (i.isHasCertificate() && c != null) {
                i.setCertBakedAt(trim(c.bakedAt()));
                i.setCertWeight(trim(c.weight()));
                i.setCertLabor(trim(c.labor()));
                i.setCertStarter(isBlank(c.starter()) ? settings.defaultStarter() : trim(c.starter()));
            }
        }
        saveMadelineMessage(o, message);
        o.changeStatus(OrderStatus.BAKED, "Baked");
        orders.save(o);
        notifier.toCustomer(o, EmailWording.BAKED, message);
    }

    @Transactional
    public void markCompleted(CustomerOrder o, String message) {
        requireStatus(o, OrderStatus.BAKED, OrderStatus.APPROVED);
        saveMadelineMessage(o, message);
        o.changeStatus(OrderStatus.COMPLETED, "Completed");
        orders.save(o);
        notifier.toCustomer(o, EmailWording.COMPLETED, message);
    }

    @Transactional
    public void setDepositPaid(CustomerOrder o, boolean paid) {
        o.setDepositPaid(paid);
        o.setUpdatedAt(Instant.now());
        orders.save(o);
    }

    @Transactional
    public void setPaid(CustomerOrder o, boolean paid) {
        o.setPaid(paid);
        o.setUpdatedAt(Instant.now());
        orders.save(o);
    }

    @Transactional
    public void sendMessage(CustomerOrder o, String message) {
        if (isBlank(message)) throw new UserFacingException("Write a message first.");
        saveMadelineMessage(o, message);
        orders.save(o);
        notifier.toCustomer(o, EmailWording.MESSAGE, message);
    }

    @Transactional
    public void logNote(CustomerOrder o, String note) {
        if (isBlank(note)) throw new UserFacingException("Write the note first.");
        o.addMessage(MessageAuthor.LOG, note.trim());
        orders.save(o);
    }

    @Transactional
    public void savePrivateNotes(CustomerOrder o, String notes) {
        o.setPrivateNotes(trim(notes));
        orders.save(o);
    }

    private void saveMadelineMessage(CustomerOrder o, String message) {
        if (!isBlank(message)) o.addMessage(MessageAuthor.MADELINE, message.trim());
    }

    private static void requireStatus(CustomerOrder o, OrderStatus... allowed) {
        for (OrderStatus s : allowed) if (o.getStatus() == s) return;
        throw new UserFacingException("This order is " + o.getStatus().getLabel().toLowerCase() + ", so that step isn't available.");
    }

    // ---- helpers ----

    private String newToken() {
        byte[] b = new byte[24];
        random.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    public static List<Long> parseIds(String csv) {
        List<Long> out = new ArrayList<>();
        if (csv == null) return out;
        for (String s : csv.split(",")) {
            try {
                if (!s.isBlank()) out.add(Long.parseLong(s.trim()));
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        return out;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
