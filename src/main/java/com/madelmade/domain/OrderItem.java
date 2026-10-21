package com.madelmade.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** One line of an order. Names and prices are copied so later menu changes never alter past orders. */
@Entity
@Table(name = "order_item")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private CustomerOrder order;
    private Long productId;
    private String productName;
    private int quantity = 1;
    private String choiceIds;
    private String optionsSummary;
    private String customNotes;
    private String lettersText;
    private String lettersFont;
    private BigDecimal unitPrice = BigDecimal.ZERO;
    private BigDecimal lineTotal = BigDecimal.ZERO;
    private boolean priceTbd;
    private boolean countsAsLoaf = true;
    private boolean hasCertificate;
    private String certName;
    private String certMessage;
    private BigDecimal certPrice = BigDecimal.ZERO;
    private String certBakedAt;
    private String certWeight;
    private String certLabor;
    private String certStarter;
    private int displayOrder;

    /** Price of the loaf itself, without the certificate add-on. */
    public BigDecimal getItemPrice() {
        return unitPrice.subtract(hasCertificate ? certPrice : BigDecimal.ZERO);
    }

    public void recalculate() {
        lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() { return id; }
    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getChoiceIds() { return choiceIds; }
    public void setChoiceIds(String choiceIds) { this.choiceIds = choiceIds; }
    public String getOptionsSummary() { return optionsSummary; }
    public void setOptionsSummary(String optionsSummary) { this.optionsSummary = optionsSummary; }
    public String getCustomNotes() { return customNotes; }
    public void setCustomNotes(String customNotes) { this.customNotes = customNotes; }
    public String getLettersText() { return lettersText; }
    public void setLettersText(String lettersText) { this.lettersText = lettersText; }
    public String getLettersFont() { return lettersFont; }
    public void setLettersFont(String lettersFont) { this.lettersFont = lettersFont; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
    public boolean isPriceTbd() { return priceTbd; }
    public void setPriceTbd(boolean priceTbd) { this.priceTbd = priceTbd; }
    public boolean isCountsAsLoaf() { return countsAsLoaf; }
    public void setCountsAsLoaf(boolean countsAsLoaf) { this.countsAsLoaf = countsAsLoaf; }
    public boolean isHasCertificate() { return hasCertificate; }
    public void setHasCertificate(boolean hasCertificate) { this.hasCertificate = hasCertificate; }
    public String getCertName() { return certName; }
    public void setCertName(String certName) { this.certName = certName; }
    public String getCertMessage() { return certMessage; }
    public void setCertMessage(String certMessage) { this.certMessage = certMessage; }
    public BigDecimal getCertPrice() { return certPrice; }
    public void setCertPrice(BigDecimal certPrice) { this.certPrice = certPrice; }
    public String getCertBakedAt() { return certBakedAt; }
    public void setCertBakedAt(String certBakedAt) { this.certBakedAt = certBakedAt; }
    public String getCertWeight() { return certWeight; }
    public void setCertWeight(String certWeight) { this.certWeight = certWeight; }
    public String getCertLabor() { return certLabor; }
    public void setCertLabor(String certLabor) { this.certLabor = certLabor; }
    public String getCertStarter() { return certStarter; }
    public void setCertStarter(String certStarter) { this.certStarter = certStarter; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
