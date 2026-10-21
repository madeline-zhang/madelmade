package com.madelmade.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.madelmade.domain.CustomerOrder;
import com.madelmade.domain.Fulfillment;
import com.madelmade.domain.OrderItem;
import com.madelmade.domain.TaxCategory;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TaxCalculatorTest {
    static final TaxCalculator.Rates ADDED = new TaxCalculator.Rates(true, false, new BigDecimal("7"), new BigDecimal("4"), new BigDecimal("2.75"));
    static final TaxCalculator.Rates INCLUDED = new TaxCalculator.Rates(true, true, new BigDecimal("7"), new BigDecimal("4"), new BigDecimal("2.75"));

    static BigDecimal d(String s) { return new BigDecimal(s); }

    @Test
    void preparedFoodWithFeesIsTaxedOnEverything() {
        // $20 of bread, $5 priority, $6 delivery.
        TaxCalculator.Result r = TaxCalculator.compute(ADDED, Map.of(TaxCategory.PREPARED, d("20")), d("31"));
        assertThat(r.taxablePrepared()).isEqualByComparingTo("31");
        assertThat(r.stateTax()).isEqualByComparingTo("2.17");
        assertThat(r.localTax()).isEqualByComparingTo("0.85");
        assertThat(r.tax()).isEqualByComparingTo("3.02");
        assertThat(TaxCalculator.label(ADDED, r)).isEqualTo("Sales tax (9.75%)");
    }

    @Test
    void mixedOrdersSplitFeesByItemAmounts() {
        TaxCalculator.Result r = TaxCalculator.compute(ADDED,
                Map.of(TaxCategory.PREPARED, d("10"), TaxCategory.FOOD, d("10")), d("24"));
        assertThat(r.taxablePrepared()).isEqualByComparingTo("12");
        assertThat(r.taxableFood()).isEqualByComparingTo("12");
        assertThat(r.stateTax()).isEqualByComparingTo("1.32");
        assertThat(r.localTax()).isEqualByComparingTo("0.66");
        assertThat(TaxCalculator.label(ADDED, r)).isEqualTo("Sales tax");
    }

    @Test
    void untaxedItemsAndSwitchedOffTax() {
        TaxCalculator.Result none = TaxCalculator.compute(ADDED, Map.of(TaxCategory.NONE, d("8")), d("8"));
        assertThat(none.tax()).isEqualByComparingTo("0");
        assertThat(none.nonTaxable()).isEqualByComparingTo("8");
        TaxCalculator.Result off = TaxCalculator.compute(TaxCalculator.Rates.OFF, Map.of(TaxCategory.PREPARED, d("8")), d("8"));
        assertThat(off.tax()).isEqualByComparingTo("0");
        assertThat(off.nonTaxable()).isEqualByComparingTo("8");
    }

    @Test
    void includedPricesBackTheTaxOut() {
        TaxCalculator.Result r = TaxCalculator.compute(INCLUDED, Map.of(TaxCategory.PREPARED, d("109.75")), d("109.75"));
        assertThat(r.taxablePrepared()).isEqualByComparingTo("100");
        assertThat(r.stateTax()).isEqualByComparingTo("7.00");
        assertThat(r.localTax()).isEqualByComparingTo("2.75");
        assertThat(TaxCalculator.label(INCLUDED, r)).isEqualTo("Includes sales tax (9.75%)");
    }

    @Test
    void orderAddsTaxAfterPromoAndRecomputesWithDeliveryFee() {
        CustomerOrder o = new CustomerOrder();
        o.setFulfillment(Fulfillment.DELIVERY);
        OrderItem i = new OrderItem();
        i.setUnitPrice(d("10"));
        i.setQuantity(2);
        i.recalculate();
        o.addItem(i);
        o.setPromoPercent(50);
        o.setPromoCode("HALF");
        o.applyTaxRates(ADDED);
        o.recalculate();
        // $20 items, 50% off = $10 taxable.
        assertThat(o.getTax()).isEqualByComparingTo("0.98");
        assertThat(o.getTotal()).isEqualByComparingTo("10.98");
        o.setDeliveryFee(d("6"));
        o.recalculate();
        assertThat(o.getTaxablePrepared()).isEqualByComparingTo("16");
        assertThat(o.getTax()).isEqualByComparingTo("1.56");
        assertThat(o.getTotal()).isEqualByComparingTo("17.56");
        // A rate change in Settings later doesn't touch the order: it kept its own rates.
        assertThat(o.getTaxRates().prepared()).isEqualByComparingTo("7");
    }
}
