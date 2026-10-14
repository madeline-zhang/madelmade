package com.madelmade.service;

import java.util.List;

/** A problem to show the person in plain words, like "Please choose a date". */
public class UserFacingException extends RuntimeException {
    private final List<String> messages;

    public UserFacingException(String message) {
        this(List.of(message));
    }

    public UserFacingException(List<String> messages) {
        super(String.join(" ", messages));
        this.messages = messages;
    }

    public List<String> getMessages() { return messages; }
}
