package com.madelmade.service;

import com.madelmade.domain.PromoCode;
import com.madelmade.repo.PromoCodeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PromoService {
    private final PromoCodeRepository codes;
    private final Formats formats;

    public PromoService(PromoCodeRepository codes, Formats formats) {
        this.codes = codes;
        this.formats = formats;
    }

    public static String normalize(String code) {
        return code == null ? "" : code.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    /** The usable code for what the customer typed, or a friendly reason it can't be used. */
    public PromoCode check(String typed) {
        String code = normalize(typed);
        if (code.isEmpty()) return null;
        Optional<PromoCode> found = codes.findByCodeIgnoreCase(code);
        if (found.isEmpty()) throw new UserFacingException("We don't recognize the promo code " + code + ". Please check the spelling.");
        String problem = found.get().problem(formats.today());
        if (problem != null) throw new UserFacingException(problem);
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
