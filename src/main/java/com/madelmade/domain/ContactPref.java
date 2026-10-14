package com.madelmade.domain;

public enum ContactPref {
    EMAIL("Email"), TEXT("Text"), WHATSAPP("WhatsApp");

    private final String label;

    ContactPref(String label) { this.label = label; }

    public String getLabel() { return label; }
}
