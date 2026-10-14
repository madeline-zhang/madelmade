package com.madelmade.service;

import com.madelmade.domain.OptionChoice;
import com.madelmade.domain.OptionGroup;
import com.madelmade.domain.Product;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Prices a product with the customer's choices. The product page does the same math in the
 * browser for the live price; this is the version that counts.
 */
@Service
public class PricingService {
    private final SettingsService settings;

    public PricingService(SettingsService settings) {
        this.settings = settings;
    }

    public record Selection(List<Long> choiceIds, String lettersText, String customNotes,
                            boolean certificate, String certName, String certMessage) {}

    public record Quote(BigDecimal unitPrice, BigDecimal certPrice, String summary, boolean priceTbd,
                        List<Long> choiceIds, List<String> errors) {
        public boolean ok() { return errors.isEmpty(); }
    }

    public Quote quote(Product product, Selection sel) {
        List<String> errors = new ArrayList<>();
        List<OptionChoice> picked = new ArrayList<>();
        List<Long> ids = sel.choiceIds() == null ? List.of() : sel.choiceIds();

        for (OptionGroup g : product.getOptionGroups()) {
            List<OptionChoice> inGroup = g.getActiveChoices().stream().filter(c -> ids.contains(c.getId())).toList();
            if (g.isRequired() && inGroup.isEmpty() && !g.getActiveChoices().isEmpty()) {
                errors.add("Please choose a " + g.getName().toLowerCase() + ".");
            }
            if (!g.isMultiple() && inGroup.size() > 1) {
                errors.add("Please pick just one " + g.getName().toLowerCase() + ".");
            }
            picked.addAll(inGroup);
        }

        BigDecimal price = product.getBasePrice();
        boolean half = false;
        boolean tbd = false;
        boolean needsText = false;
        Integer textLimit = null;
        for (OptionChoice c : picked) {
            price = price.add(c.getPriceChange());
            half |= c.isHalvesPrice();
            tbd |= c.isPriceTbd();
            if (c.getTextLimit() != null && c.getTextLimit() > 0) {
                needsText = true;
                textLimit = c.getTextLimit();
            }
        }
        if (half) price = price.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        String letters = sel.lettersText() == null ? "" : sel.lettersText().trim();
        if (needsText) {
            if (letters.isEmpty()) errors.add("Please type the letters you'd like on your loaf.");
            else if (letters.length() > textLimit) errors.add("Letters can be up to " + textLimit + " characters.");
        }
        boolean custom = picked.stream().anyMatch(OptionChoice::isCustom);
        if (custom && (sel.customNotes() == null || sel.customNotes().isBlank())) {
            errors.add("Please describe your custom idea so Madeline can price it.");
        }

        BigDecimal certPrice = BigDecimal.ZERO;
        if (sel.certificate()) {
            if (!product.isAllowsBirthCertificate() || !settings.certificatesEnabled()) {
                errors.add("Birth certificates aren't available for this item.");
            } else {
                if (sel.certName() == null || sel.certName().isBlank()) errors.add("Please give your bread a name for its birth certificate.");
                certPrice = settings.certificatePrice();
                price = price.add(certPrice);
            }
        }

        String summary = picked.stream().map(c -> {
            String s = c.getName();
            if (c.getTextLimit() != null && !letters.isEmpty()) s += " \"" + letters + "\"";
            return s;
        }).collect(Collectors.joining(" · "));

        return new Quote(price, certPrice, summary, tbd, picked.stream().map(OptionChoice::getId).toList(), errors);
    }

    /** The lowest price on the card ("from $10"), ignoring the half-size discount. */
    public BigDecimal fromPrice(Product p) {
        return p.getBasePrice();
    }
}
