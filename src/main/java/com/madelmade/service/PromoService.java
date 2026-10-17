package com.madelmade.service;

import com.madelmade.domain.OrderStatus;
import com.madelmade.domain.PromoCode;
import com.madelmade.repo.OrderRepository;
import com.madelmade.repo.PromoCodeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PromoService {
    private final PromoCodeRepository codes;
    private final OrderRepository orders;
    private final Formats formats;

    public PromoService(PromoCodeRepository codes, OrderRepository orders, Formats formats) {
        this.codes = codes;
        this.orders = orders;
        this.formats = formats;
    }

    public static String normalize(String code) {
        return code == null ? "" : code.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    /** The usable code for what the customer typed, or a friendly reason it can't be used. */
    public PromoCode check(String typed) {
        return check(typed, null);
    }

    /** Same as {@link #check(String)}, also turning away a second use of a one-per-customer code. */
    public PromoCode check(String typed, String email) {
        String code = normalize(typed);
        if (code.isEmpty()) return null;
        Optional<PromoCode> found = codes.findByCodeIgnoreCase(code);
        if (found.isEmpty()) throw new UserFacingException("We don't recognize the promo code " + code + ". Please check the spelling.");
        String problem = found.get().problem(formats.today());
        if (problem != null) throw new UserFacingException(problem);
        if (found.get().isOncePerCustomer() && email != null && !email.isBlank()
                && orders.countPromoUses(code, email.trim(), EnumSet.of(OrderStatus.CANCELLED, OrderStatus.DECLINED)) > 0) {
            throw new UserFacingException("You've already used the promo code " + code + ". It's one per customer.");
        }
        return found.get();
    }

    /** What a code takes off an amount, rounded to cents. */
    public static BigDecimal discountOn(BigDecimal amount, int percent) {
        return amount.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public void recordUse(PromoCode code) {
        code.setTimesUsed(code.getTimesUsed() + 1);
        codes.save(code);
    }
}
