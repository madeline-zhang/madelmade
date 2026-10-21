package com.madelmade.web.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderingTest {
    static final class Item {
        final String name;
        int order;
        Item(String name, int order) { this.name = name; this.order = order; }
    }

    private static List<String> sorted(List<Item> items) {
        List<Item> copy = new ArrayList<>(items);
        copy.sort((a, b) -> Integer.compare(a.order, b.order));
        return copy.stream().map(i -> i.name).toList();
    }

    @Test
    void movesAnItemAndRenumbersEveryone() {
        Item a = new Item("a", 5), b = new Item("b", 5), c = new Item("c", 9);
        List<Item> list = List.of(a, b, c);
        Ordering.move(list, c, "up", (i, n) -> i.order = n);
        assertThat(sorted(list)).containsExactly("a", "c", "b");
        assertThat(List.of(a.order, c.order, b.order)).containsExactly(0, 1, 2);
        Ordering.move(List.of(a, c, b), a, "up", (i, n) -> i.order = n); // already first: stays put
        assertThat(sorted(list)).containsExactly("a", "c", "b");
    }
}
