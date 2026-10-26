package com.madelmade.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.madelmade.domain.CustomerOrder;
import com.madelmade.domain.Fulfillment;
import com.madelmade.domain.OrderItem;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ShippingTest {
    @Test
    void onlyTennesseeZipCodesShip() {
        assertThat(OrderService.isTennesseeAddress("12 Oak St, Brentwood, TN 37027")).isTrue();
        assertThat(OrderService.isTennesseeAddress("400 Poplar Ave\nMemphis TN 38103-1234")).isTrue();
        // A five-digit street number doesn't count; the ZIP at the end does.
        assertThat(OrderService.isTennesseeAddress("37027 Main St, Atlanta, GA 30301")).isFalse();
        assertThat(OrderService.isTennesseeAddress("1 Main St, Bowling Green, KY 42101")).isFalse();
        assertThat(OrderService.isTennesseeAddress("12 Oak St, Brentwood, TN")).isFalse();
    }

    @Test
    void aShippedOrderIsPaidInFullUpFront() {
        CustomerOrder o = new CustomerOrder();
        o.setFulfillment(Fulfillment.SHIPPING);
        OrderItem i = new OrderItem();
        i.setUnitPrice(new BigDecimal("12"));
        i.setQuantity(1);
        i.recalculate();
        o.addItem(i);
        o.setDeliveryFee(new BigDecimal("9"));
        o.recalculate();
        assertThat(o.isAddressed()).isTrue();
        assertThat(o.isDepositRequired()).isTrue();
        assertThat(o.getFeeName()).isEqualTo("Shipping");
        assertThat(o.getDepositAmount()).isEqualByComparingTo("21");

        o.setFulfillment(Fulfillment.DELIVERY);
        assertThat(o.getDepositAmount()).isEqualByComparingTo("9");
    }

    @Test
    void drinkOnlyOrdersHaveNothingToBake() {
        CustomerOrder o = new CustomerOrder();
        OrderItem latte = new OrderItem();
        latte.setUnitPrice(new BigDecimal("6"));
        latte.setQuantity(2);
        latte.setCountsAsLoaf(false);
        latte.recalculate();
        o.addItem(latte);
        assertThat(o.isNeedsBaking()).isFalse();
        OrderItem loaf = new OrderItem();
        loaf.setUnitPrice(new BigDecimal("12"));
        loaf.setQuantity(1);
        loaf.setCountsAsLoaf(true);
        loaf.recalculate();
        o.addItem(loaf);
        assertThat(o.isNeedsBaking()).isTrue();
    }
}
