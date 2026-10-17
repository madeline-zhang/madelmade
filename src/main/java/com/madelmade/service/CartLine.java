package com.madelmade.service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CartLine implements Serializable {
    private final String id = UUID.randomUUID().toString().substring(0, 8);
    private Long productId;
    private boolean pickupOnly;
    private String productName;
    private String slug;
    private Long photoId;
    private List<Long> choiceIds = new ArrayList<>();
    private String optionsSummary;
    private String customNotes;
    private String lettersText;
    private boolean certificate;
    private String certName;
    private String certMessage;
    private BigDecimal certPrice = BigDecimal.ZERO;
    private BigDecimal unitPrice = BigDecimal.ZERO;
    private int quantity = 1;
    private boolean priceTbd;
    private boolean countsAsLoaf;

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getItemPrice() {
        return unitPrice.subtract(certificate ? certPrice : BigDecimal.ZERO);
    }

    public String getId() { return id; }
    public boolean isPickupOnly() { return pickupOnly; }
    public void setPickupOnly(boolean pickupOnly) { this.pickupOnly = pickupOnly; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public Long getPhotoId() { return photoId; }
    public void setPhotoId(Long photoId) { this.photoId = photoId; }
    public List<Long> getChoiceIds() { return choiceIds; }
    public void setChoiceIds(List<Long> choiceIds) { this.choiceIds = choiceIds; }
    public String getOptionsSummary() { return optionsSummary; }
    public void setOptionsSummary(String optionsSummary) { this.optionsSummary = optionsSummary; }
    public String getCustomNotes() { return customNotes; }
    public void setCustomNotes(String customNotes) { this.customNotes = customNotes; }
    public String getLettersText() { return lettersText; }
    public void setLettersText(String lettersText) { this.lettersText = lettersText; }
    public boolean isCertificate() { return certificate; }
    public void setCertificate(boolean certificate) { this.certificate = certificate; }
    public String getCertName() { return certName; }
    public void setCertName(String certName) { this.certName = certName; }
    public String getCertMessage() { return certMessage; }
    public void setCertMessage(String certMessage) { this.certMessage = certMessage; }
    public BigDecimal getCertPrice() { return certPrice; }
    public void setCertPrice(BigDecimal certPrice) { this.certPrice = certPrice; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public boolean isPriceTbd() { return priceTbd; }
    public void setPriceTbd(boolean priceTbd) { this.priceTbd = priceTbd; }
    public boolean isCountsAsLoaf() { return countsAsLoaf; }
    public void setCountsAsLoaf(boolean countsAsLoaf) { this.countsAsLoaf = countsAsLoaf; }
}
