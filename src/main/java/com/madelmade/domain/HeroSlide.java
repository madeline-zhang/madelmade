package com.madelmade.domain;

import jakarta.persistence.*;

/** One picture in the home page hero: a photo (computer crop, optional phone crop) or a looping video. */
@Entity
@Table(name = "hero_slide")
public class HeroSlide {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long photoId;
    private Long phonePhotoId;
    private Long videoId;
    private String alt;
    private int displayOrder;

    public Long getId() { return id; }
    public Long getPhotoId() { return photoId; }
    public void setPhotoId(Long photoId) { this.photoId = photoId; }
    public Long getPhonePhotoId() { return phonePhotoId; }
    public void setPhonePhotoId(Long phonePhotoId) { this.phonePhotoId = phonePhotoId; }
    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }
    public String getAlt() { return alt; }
    public void setAlt(String alt) { this.alt = alt; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public boolean isVideo() { return videoId != null; }
}
