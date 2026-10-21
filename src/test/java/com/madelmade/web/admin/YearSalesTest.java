package com.madelmade.web.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class YearSalesTest {
    @Test
    void quietUnderThreeThousand() {
        assertNull(YearSales.of(2026, new BigDecimal("2999.99"), false).message());
    }

    @Test
    void licenseNoteFromThreeThousand() {
        assertEquals("info", YearSales.of(2026, new BigDecimal("3000"), false).level());
    }

    @Test
    void taxWarningNearAndOverLimit() {
        assertEquals("error", YearSales.of(2026, new BigDecimal("4000"), false).level());
        YearSales over = YearSales.of(2026, new BigDecimal("4800.01"), false);
        assertEquals(true, over.message().startsWith("Sales are over $4,800"));
    }

    @Test
    void noTaxWarningOnceChargingTax() {
        assertEquals("info", YearSales.of(2026, new BigDecimal("6000"), true).level());
    }
}
