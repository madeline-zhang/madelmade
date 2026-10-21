package com.madelmade.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A set of choices on a product, like Size, Flavor, Score style or Milk. A shared group has no
 * product of its own and is used by several products (see Product.sharedGroups).
 */
@Entity
@Table(name = "option_group")
public class OptionGroup {
    public static final String STYLE_BUTTONS = "buttons";
    public static final String STYLE_SWATCHES = "swatches";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER)
    private Product product;
    private String name;
    private boolean required = true;
    private boolean multiple;
    private String displayStyle = STYLE_BUTTONS;
    private int displayOrder;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder, id")
    private List<OptionChoice> choices = new ArrayList<>();

    public List<OptionChoice> getActiveChoices() {
        return choices.stream().filter(OptionChoice::isActive).toList();
    }

    public boolean isShared() {
        return product == null;
    }

    public boolean isSwatches() {
        return STYLE_SWATCHES.equals(displayStyle);
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public boolean isMultiple() { return multiple; }
    public void setMultiple(boolean multiple) { this.multiple = multiple; }
    public String getDisplayStyle() { return displayStyle; }
    public void setDisplayStyle(String displayStyle) { this.displayStyle = displayStyle; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public List<OptionChoice> getChoices() { return choices; }
}
