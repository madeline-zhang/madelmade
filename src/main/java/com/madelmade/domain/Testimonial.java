package com.madelmade.domain;

import jakarta.persistence.*;

/** A customer's quote for the home page. The name and the line about what they ordered are optional. */
@Entity
@Table(name = "testimonial")
public class Testimonial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String quote;
    private String name;
    private String detail;
    private int displayOrder;

    public Long getId() { return id; }
    public String getQuote() { return quote; }
    public void setQuote(String quote) { this.quote = quote; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    /** "Sarah · Birthday loaf", either part alone, or empty. */
    public String getByline() {
        boolean n = name != null && !name.isBlank(), d = detail != null && !detail.isBlank();
        return n && d ? name + " · " + detail : n ? name : d ? detail : "";
    }
}
