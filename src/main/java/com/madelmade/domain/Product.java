package com.madelmade.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
    private boolean allowsBirthCertificate;
    private String badge;
    private Long photoId;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder, id")
    private List<OptionGroup> optionGroups = new ArrayList<>();

    /** True when the product has choices to make, so the card shows "from $X". */
    public boolean isBuildYourOwn() {
        return !optionGroups.isEmpty();
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
}
