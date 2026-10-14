package com.madelmade.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** Formats prices like the menu does: $10, $5.50. Used in templates as @money.fmt(x). */
@Component("money")
public class Money {
    public String fmt(BigDecimal value) {
        if (value == null) return "";
        BigDecimal v = value.setScale(2, RoundingMode.HALF_UP);
        String sign = v.signum() < 0 ? "-" : "";
        v = v.abs();
        if (v.stripTrailingZeros().scale() <= 0) return sign + "$" + v.toBigInteger();
        return sign + "$" + v.toPlainString();
    }

    /** "+$2" for option price changes, empty for free choices. */
    public String change(BigDecimal value) {
        if (value == null || value.signum() == 0) return "";
        return (value.signum() > 0 ? "+" : "") + fmt(value);
    }

    public String plain(BigDecimal value) {
        return value == null ? "" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
