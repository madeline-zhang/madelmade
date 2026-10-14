package com.madelmade.config;

import java.io.Serializable;

/** The signed-in person when they used an emailed code instead of Google. */
public record AppUser(String email, String name) implements Serializable {}
