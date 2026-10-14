package com.madelmade.domain;

public enum MessageAuthor {
    /** Sent by Madeline from the dashboard; the customer sees it and is notified. */
    MADELINE,
    /** Written by the customer on their status page. */
    CUSTOMER,
    /** Logged by Madeline from a text or WhatsApp chat so the record stays complete. */
    LOG
}
