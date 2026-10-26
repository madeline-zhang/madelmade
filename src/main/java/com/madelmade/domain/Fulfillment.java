package com.madelmade.domain;

public enum Fulfillment {
    PICKUP("Pickup"), DELIVERY("Delivery"), SHIPPING("Shipping");

    private final String label;

    Fulfillment(String label) { this.label = label; }

    public String getLabel() { return label; }
}
