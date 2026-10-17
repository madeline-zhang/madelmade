package com.madelmade.service;

import java.util.List;
import java.util.Map;

/** A problem to show the person in plain words, like "Please choose a date". */
public class UserFacingException extends RuntimeException {
    private final List<String> messages;
    /** The same messages grouped by the part of the form they belong to, so each shows beside its field. */
    private final Map<String, List<String>> byField;

    public UserFacingException(String message) {
        this(List.of(message));
    }

    public UserFacingException(List<String> messages) {
        this(messages, Map.of());
    }

    public UserFacingException(List<String> messages, Map<String, List<String>> byField) {
        super(String.join(" ", messages));
        this.messages = messages;
        this.byField = byField;
    }

    public List<String> getMessages() { return messages; }
    public Map<String, List<String>> getByField() { return byField; }
}
