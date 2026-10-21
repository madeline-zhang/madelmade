package com.madelmade.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "gallery_photo")
public class GalleryPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long photoId;
    private String caption;
    private int displayOrder;
    /** "loaves" for past loaves, "other" for the Other bakes section. */
    private String section = LOAVES;

    public static final String LOAVES = "loaves";
    public static final String OTHER = "other";

    public Long getId() { return id; }
    public Long getPhotoId() { return photoId; }
    public void setPhotoId(Long photoId) { this.photoId = photoId; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public boolean isOther() { return OTHER.equals(section); }
}
