package com.madelmade.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "customer")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String phone;
    @Enumerated(EnumType.STRING)
    private ContactPref contactPref = ContactPref.EMAIL;
    private String address;
    private String googleId;
    private Instant createdAt = Instant.now();
    private Instant lastSignIn;

    public String getFirstName() {
        if (name == null || name.isBlank()) return "there";
        return name.trim().split("\\s+")[0];
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public ContactPref getContactPref() { return contactPref; }
    public void setContactPref(ContactPref contactPref) { this.contactPref = contactPref; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastSignIn() { return lastSignIn; }
    public void setLastSignIn(Instant lastSignIn) { this.lastSignIn = lastSignIn; }
}
