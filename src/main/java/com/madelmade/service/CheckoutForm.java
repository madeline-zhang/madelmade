package com.madelmade.service;

import com.madelmade.domain.ContactPref;
import com.madelmade.domain.Fulfillment;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class CheckoutForm {
    private String name;
    private String email;
    private String phone;
    private ContactPref contactPref = ContactPref.EMAIL;
    private Fulfillment fulfillment = Fulfillment.PICKUP;
    private String address;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
    private String time;
    private boolean priority;
    private String note;
    private String promoCode;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public ContactPref getContactPref() { return contactPref; }
    public void setContactPref(ContactPref contactPref) { this.contactPref = contactPref; }
    public Fulfillment getFulfillment() { return fulfillment; }
    public void setFulfillment(Fulfillment fulfillment) { this.fulfillment = fulfillment; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public boolean isPriority() { return priority; }
    public void setPriority(boolean priority) { this.priority = priority; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }
}
