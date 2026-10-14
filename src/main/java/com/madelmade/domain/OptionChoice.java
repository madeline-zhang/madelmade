package com.madelmade.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "option_choice")
public class OptionChoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private OptionGroup group;
    private String name;
    private BigDecimal priceChange = BigDecimal.ZERO;
    /** "Half" size: the loaf costs half the full price. */
    private boolean halvesPrice;
    /** Custom choices: Madeline sets the final price before approving. */
    private boolean priceTbd;
    private boolean needsDesignApproval;
    private String description;
    private Long photoId;
    /** When set, the customer types text up to this many characters (e.g. 15 for Letters). */
    private Integer textLimit;
    @Column(name = "is_default")
    private boolean defaultChoice;
    private boolean active = true;
    private int displayOrder;

    public boolean isCustom() {
        return priceTbd || needsDesignApproval;
    }

    public Long getId() { return id; }
    public OptionGroup getGroup() { return group; }
    public void setGroup(OptionGroup group) { this.group = group; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPriceChange() { return priceChange; }
    public void setPriceChange(BigDecimal priceChange) { this.priceChange = priceChange; }
    public boolean isHalvesPrice() { return halvesPrice; }
    public void setHalvesPrice(boolean halvesPrice) { this.halvesPrice = halvesPrice; }
    public boolean isPriceTbd() { return priceTbd; }
    public void setPriceTbd(boolean priceTbd) { this.priceTbd = priceTbd; }
    public boolean isNeedsDesignApproval() { return needsDesignApproval; }
    public void setNeedsDesignApproval(boolean needsDesignApproval) { this.needsDesignApproval = needsDesignApproval; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getPhotoId() { return photoId; }
    public void setPhotoId(Long photoId) { this.photoId = photoId; }
    public Integer getTextLimit() { return textLimit; }
    public void setTextLimit(Integer textLimit) { this.textLimit = textLimit; }
    public boolean isDefaultChoice() { return defaultChoice; }
    public void setDefaultChoice(boolean defaultChoice) { this.defaultChoice = defaultChoice; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
