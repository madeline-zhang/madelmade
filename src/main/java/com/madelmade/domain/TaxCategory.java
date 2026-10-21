package com.madelmade.domain;

/** How Tennessee taxes a product. Each rate is set in Settings; the local rate applies to both taxed kinds. */
public enum TaxCategory {
    PREPARED("Prepared food"),
    FOOD("Food or grocery"),
    NONE("Not taxed");

    private final String label;

    TaxCategory(String label) { this.label = label; }

    public String getLabel() { return label; }
}
