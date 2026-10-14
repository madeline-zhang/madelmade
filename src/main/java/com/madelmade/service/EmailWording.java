package com.madelmade.service;

/** Customer emails whose wording Madeline can edit in Settings. */
public enum EmailWording {
    RECEIVED("Request received", "We got your MadelMade request",
            "Thank you for your request! Madeline will look it over and get back to you soon. Nothing is charged until she confirms."),
    APPROVED("Approved", "Your MadelMade order is confirmed",
            "Good news: Madeline approved your order. Here are the details. You pay at pickup or delivery (cash, Venmo or Zelle)."),
    CHANGES("Needs changes", "A quick question about your MadelMade order",
            "Madeline has a question about your request before she can confirm it. Reply on your order page and it goes straight to her."),
    DECLINED("Declined", "About your MadelMade request",
            "Madeline isn't able to bake this request, and she's sorry about that. Her note is below."),
    BAKED("Baked", "Your loaf is baked!",
            "Your loaf just came out of the oven. Madeline will confirm the pickup or delivery time with you."),
    COMPLETED("Completed", "Thank you from MadelMade",
            "Thank you for ordering! We hope it made you smile. Whenever you want another, ordering again takes a minute."),
    REMINDER("Day-before reminder", "See you tomorrow: your MadelMade order",
            "A friendly reminder that your order is planned for tomorrow."),
    MESSAGE("New message", "A message from Madeline about your order",
            "Madeline sent you a message about your order.");

    public final String label;
    public final String defaultSubject;
    public final String defaultIntro;

    EmailWording(String label, String defaultSubject, String defaultIntro) {
        this.label = label;
        this.defaultSubject = defaultSubject;
        this.defaultIntro = defaultIntro;
    }

    public String key() { return name().toLowerCase(); }
    public String subjectKey() { return "email." + key() + ".subject"; }
    public String introKey() { return "email." + key() + ".intro"; }
    public String getLabel() { return label; }
}
