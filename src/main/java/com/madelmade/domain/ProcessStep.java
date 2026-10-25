package com.madelmade.domain;

import jakarta.persistence.*;

/** One step on the How it's made page: a photo or looping video with a title and a few lines. */
@Entity
@Table(name = "process_step")
public class ProcessStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String body;
    private Long photoId;
    private Long videoId;
    private int displayOrder;

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Long getPhotoId() { return photoId; }
    public void setPhotoId(Long photoId) { this.photoId = photoId; }
    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public boolean isVideo() { return videoId != null; }

    /** The text split at blank lines. */
    public java.util.List<String> getParagraphs() {
        if (body == null) return java.util.List.of();
        return java.util.Arrays.stream(body.replace("\r", "").split("\n\\s*\n")).map(String::trim).filter(p -> !p.isEmpty()).toList();
    }
}
