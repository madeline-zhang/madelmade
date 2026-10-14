package com.madelmade.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A photo with its untouched original and the edited version shown on the site.
 * The edit settings are saved so Madeline can re-crop or change warmth later.
 */
@Entity
@Table(name = "photo")
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private byte[] original;
    private String originalType;
    private byte[] edited;
    private String editedType = "image/jpeg";
    private int rotate;
    private double tilt;
    private String crop = "original";
    private int warmth;
    private int brightness;
    private Integer width;
    private Integer height;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public byte[] getOriginal() { return original; }
    public void setOriginal(byte[] original) { this.original = original; }
    public String getOriginalType() { return originalType; }
    public void setOriginalType(String originalType) { this.originalType = originalType; }
    public byte[] getEdited() { return edited; }
    public void setEdited(byte[] edited) { this.edited = edited; }
    public String getEditedType() { return editedType; }
    public void setEditedType(String editedType) { this.editedType = editedType; }
    public int getRotate() { return rotate; }
    public void setRotate(int rotate) { this.rotate = rotate; }
    public double getTilt() { return tilt; }
    public void setTilt(double tilt) { this.tilt = tilt; }
    public String getCrop() { return crop; }
    public void setCrop(String crop) { this.crop = crop; }
    public int getWarmth() { return warmth; }
    public void setWarmth(int warmth) { this.warmth = warmth; }
    public int getBrightness() { return brightness; }
    public void setBrightness(int brightness) { this.brightness = brightness; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
