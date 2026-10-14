package com.madelmade.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "order_message")
public class OrderMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private CustomerOrder order;
    @Enumerated(EnumType.STRING)
    private MessageAuthor author;
    private String body;
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }
    public MessageAuthor getAuthor() { return author; }
    public void setAuthor(MessageAuthor author) { this.author = author; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Instant getCreatedAt() { return createdAt; }
}
