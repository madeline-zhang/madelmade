package com.madelmade.service;

import org.springframework.stereotype.Service;

@Service
public class AdminAccess {
    private final SettingsService settings;

    public AdminAccess(SettingsService settings) {
        this.settings = settings;
    }

    public boolean isAdmin(String email) {
        return email != null && settings.adminEmails().contains(email.trim().toLowerCase());
    }
}
