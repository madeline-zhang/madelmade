package com.madelmade.service;

import com.madelmade.domain.TaxCategory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Sales tax for one order. Everything the customer pays for is in the taxable base (items after the
 * promo discount, the priority fee, the delivery fee and any adjustment); when an order mixes kinds of
 * product, that base is split between them in proportion to their item amounts. Rounded per order.
 */
public final class TaxCalculator {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private TaxCalculator() {}

    /** Rates as percentages, e.g. 7, 4 and 2.75. */
    public record Rates(boolean enabled, boolean included, BigDecimal prepared, BigDecimal food, BigDecimal local) {
        public static final Rates OFF = new Rates(false, false, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        public BigDecimal combined(TaxCategory c) {
            return switch (c) {
                case PREPARED -> prepared.add(local);
                case FOOD -> food.add(local);
                case NONE -> BigDecimal.ZERO;
            };
        }
    }

    /** Taxable amounts are before tax, also when prices include it. */
    public record Result(BigDecimal taxablePrepared, BigDecimal taxableFood, BigDecimal nonTaxable,
                         BigDecimal stateTax, BigDecimal localTax) {
        public static final Result NONE = new Result(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        public BigDecimal tax() { return stateTax.add(localTax); }
    }

    /**
     * @param itemsByCategory item amounts (line totals) per kind of product
     * @param charged everything the customer pays before tax; with prices that include tax, everything they pay
     */
    public static Result compute(Rates r, Map<TaxCategory, BigDecimal> itemsByCategory, BigDecimal charged) {
        if (charged == null || charged.signum() <= 0) return Result.NONE;
        if (r == null || !r.enabled()) return new Result(BigDecimal.ZERO, BigDecimal.ZERO, cents(charged), BigDecimal.ZERO, BigDecimal.ZERO);

        BigDecimal prepared = itemsByCategory.getOrDefault(TaxCategory.PREPARED, BigDecimal.ZERO);
        BigDecimal food = itemsByCategory.getOrDefault(TaxCategory.FOOD, BigDecimal.ZERO);
        BigDecimal none = itemsByCategory.getOrDefault(TaxCategory.NONE, BigDecimal.ZERO);
        BigDecimal items = prepared.add(food).add(none);
        BigDecimal basePrepared, baseFood;
        if (items.signum() <= 0) {
            // Only custom-priced items so far: use whichever kind is in the order, prepared food first.
            boolean hasPrepared = itemsByCategory.containsKey(TaxCategory.PREPARED);
            boolean hasFood = itemsByCategory.containsKey(TaxCategory.FOOD);
            basePrepared = hasPrepared || !hasFood && !itemsByCategory.containsKey(TaxCategory.NONE) ? charged : BigDecimal.ZERO;
            baseFood = !hasPrepared && hasFood ? charged : BigDecimal.ZERO;
        } else {
            basePrepared = cents(charged.multiply(prepared).divide(items, 10, RoundingMode.HALF_UP));
            baseFood = cents(charged.multiply(food).divide(items, 10, RoundingMode.HALF_UP));
        }
        BigDecimal baseNone = charged.subtract(basePrepared).subtract(baseFood);

        if (!r.included()) {
            BigDecimal state = cents(pct(basePrepared, r.prepared()).add(pct(baseFood, r.food())));
            BigDecimal local = cents(pct(basePrepared.add(baseFood), r.local()));
            return new Result(cents(basePrepared), cents(baseFood), cents(baseNone), state, local);
        }
        // Prices include tax: back the tax out of each part, then split it between state and local.
        BigDecimal netPrepared = backOut(basePrepared, r.combined(TaxCategory.PREPARED));
        BigDecimal netFood = backOut(baseFood, r.combined(TaxCategory.FOOD));
        BigDecimal tax = basePrepared.subtract(netPrepared).add(baseFood.subtract(netFood));
        BigDecimal state = cents(share(basePrepared.subtract(netPrepared), r.prepared(), r.combined(TaxCategory.PREPARED))
                .add(share(baseFood.subtract(netFood), r.food(), r.combined(TaxCategory.FOOD))));
        return new Result(netPrepared, netFood, cents(baseNone), state, cents(tax).subtract(state));
    }

    /** "Sales tax (9.75%)" when every taxed part shares one rate, otherwise "Sales tax"; "Includes sales tax..." when included. */
    public static String label(Rates r, Result res) {
        java.util.Set<BigDecimal> rates = new java.util.TreeSet<>();
        if (res.taxablePrepared().signum() != 0) rates.add(r.combined(TaxCategory.PREPARED));
        if (res.taxableFood().signum() != 0) rates.add(r.combined(TaxCategory.FOOD));
        String rate = rates.size() == 1 ? " (" + rates.iterator().next().stripTrailingZeros().toPlainString() + "%)" : "";
        return (r.included() ? "Includes sales tax" : "Sales tax") + rate;
    }

    private static BigDecimal pct(BigDecimal amount, BigDecimal rate) {
        return amount.multiply(rate).divide(HUNDRED, 10, RoundingMode.HALF_UP);
    }

    private static BigDecimal backOut(BigDecimal gross, BigDecimal rate) {
        if (gross.signum() == 0) return BigDecimal.ZERO.setScale(2);
        return cents(gross.multiply(HUNDRED).divide(HUNDRED.add(rate), 10, RoundingMode.HALF_UP));
    }

    private static BigDecimal share(BigDecimal tax, BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) return BigDecimal.ZERO;
        return tax.multiply(part).divide(whole, 10, RoundingMode.HALF_UP);
    }

    static BigDecimal cents(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
