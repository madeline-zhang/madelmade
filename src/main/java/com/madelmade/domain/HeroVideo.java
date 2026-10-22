package com.madelmade.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** A hero video, kept apart from the slides so listing slides never loads the video itself. */
@Entity
@Table(name = "hero_video")
public class HeroVideo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private byte[] data;
    private String contentType;
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
