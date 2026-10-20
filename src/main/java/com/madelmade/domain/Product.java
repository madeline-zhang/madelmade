package com.madelmade.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Category category;
    private String name;
    private String slug;
    private String shortDescription;
    private String description;
    private BigDecimal basePrice = BigDecimal.ZERO;
    /** Days of notice this product needs; null means the standard notice from Settings. */
    private Integer leadTimeDays;
    private Integer weeklyLimit;
    private boolean active = true;
    private boolean soldOut;
    private int displayOrder;
    private boolean countsAsLoaf = true;
    private boolean pickupOnly;
    @Column(columnDefinition = "text")
    private String ingredients;
    private String allergens;
    private String notesHint;
    private boolean allowsBirthCertificate;
    private String badge;
    private Long photoId;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder, id")
    private List<OptionGroup> optionGroups = new ArrayList<>();

    /** Shared groups this product uses, edited once on the Shared options page. */
    @ManyToMany
    @JoinTable(name = "product_shared_group", joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id"))
    private Set<OptionGroup> sharedGroups = new HashSet<>();

    /** The product's own groups plus the shared ones it uses, in display order. */
    public List<OptionGroup> getAllOptionGroups() {
        List<OptionGroup> all = new ArrayList<>(optionGroups);
        all.addAll(sharedGroups);
        all.sort(Comparator.comparingInt(OptionGroup::getDisplayOrder).thenComparing(OptionGroup::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        return all;
    }

    /** True when the product has choices to make, so the card shows "from $X". */
    public boolean isBuildYourOwn() {
        return !optionGroups.isEmpty() || !sharedGroups.isEmpty();
    }

    public Long getId() { return id; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public Integer getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(Integer leadTimeDays) { this.leadTimeDays = leadTimeDays; }
    public Integer getWeeklyLimit() { return weeklyLimit; }
    public void setWeeklyLimit(Integer weeklyLimit) { this.weeklyLimit = weeklyLimit; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isSoldOut() { return soldOut; }
    public void setSoldOut(boolean soldOut) { this.soldOut = soldOut; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public boolean isCountsAsLoaf() { return countsAsLoaf; }
    public void setCountsAsLoaf(boolean countsAsLoaf) { this.countsAsLoaf = countsAsLoaf; }
    public boolean isPickupOnly() { return pickupOnly; }
    public void setPickupOnly(boolean pickupOnly) { this.pickupOnly = pickupOnly; }
    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }
    public String getAllergens() { return allergens; }
    public String getNotesHint() { return notesHint; }
    public void setNotesHint(String notesHint) { this.notesHint = notesHint; }
    public void setAllergens(String allergens) { this.allergens = allergens; }
    /** Tennessee's home food law needs ingredients and allergens listed for anything sold online. */
    public boolean isMissingLabelInfo() { return ingredients == null || allergens == null; }
    /** True when the ingredients box has something to show, from the product or its choices. */
    public boolean isHasLabelInfo() {
        return ingredients != null || allergens != null || getAllOptionGroups().stream()
                .flatMap(g -> g.getActiveChoices().stream())
                .anyMatch(c -> c.getIngredients() != null || c.getAllergens() != null);
    }
    public boolean isAllowsBirthCertificate() { return allowsBirthCertificate; }
    public void setAllowsBirthCertificate(boolean allowsBirthCertificate) { this.allowsBirthCertificate = allowsBirthCertificate; }
    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }
    public Long getPhotoId() { return photoId; }
    public void setPhotoId(Long photoId) { this.photoId = photoId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<OptionGroup> getOptionGroups() { return optionGroups; }
    public Set<OptionGroup> getSharedGroups() { return sharedGroups; }
}
