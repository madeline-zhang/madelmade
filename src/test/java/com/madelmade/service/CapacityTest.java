package com.madelmade.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CapacityTest {
    private static final LocalDate FRI = LocalDate.of(2026, 11, 6);

    @Test
    void countsLoavesDueWithinTheStretchAroundADate() {
        // Two loaves Wednesday, one Sunday.
        Map<LocalDate, Integer> due = Map.of(FRI.minusDays(2), 2, FRI.plusDays(2), 1);
        // Wed-Fri holds 2; Fri-Sun holds 1.
        assertThat(OrderService.busiestStretch(due, FRI, 3)).isEqualTo(2);
        // Saturday's stretches (Thu-Sat, Fri-Sun, Sat-Mon) never reach Wednesday.
        assertThat(OrderService.busiestStretch(due, FRI.plusDays(1), 3)).isEqualTo(1);
        // An order weeks away doesn't count.
        assertThat(OrderService.busiestStretch(Map.of(FRI.plusDays(20), 3), FRI, 3)).isZero();
    }

    @Test
    void aOneDayStretchIsJustThatDay() {
        Map<LocalDate, Integer> due = Map.of(FRI, 2, FRI.plusDays(1), 3);
        assertThat(OrderService.busiestStretch(due, FRI, 1)).isEqualTo(2);
    }
}
