package com.madelmade.web.admin;

import java.math.BigDecimal;

/**
 * This calendar year's gross sales and the Tennessee thresholds Madeline needs to watch:
 * a Williamson County Minimal Activity License from $3,000, and sales tax registration above $4,800.
 */
record YearSales(int year, BigDecimal gross, String level, String message) {
    static final BigDecimal LICENSE = new BigDecimal("3000");
    static final BigDecimal TAX = new BigDecimal("4800");
    static final BigDecimal TAX_NEAR = new BigDecimal("4000");

    static YearSales of(int year, BigDecimal gross, boolean taxOn) {
        if (!taxOn && gross.compareTo(TAX) > 0) {
            return new YearSales(year, gross, "error", "Sales are over $4,800 this year. Register for Tennessee sales tax, then turn on Charge sales tax in Settings. A Williamson County Minimal Activity License is also needed above $3,000.");
        }
        if (!taxOn && gross.compareTo(TAX_NEAR) >= 0) {
            return new YearSales(year, gross, "error", "Sales are nearing $4,800 this year. Above that, register for Tennessee sales tax and turn on Charge sales tax in Settings. A Williamson County Minimal Activity License is also needed above $3,000.");
        }
        if (gross.compareTo(LICENSE) >= 0) {
            return new YearSales(year, gross, "info", "Sales are over $3,000 this year, so a Williamson County Minimal Activity License is needed.");
        }
        return new YearSales(year, gross, null, null);
    }
}
