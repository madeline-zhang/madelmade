package com.madelmade.web.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ObjIntConsumer;

/** The up and down arrows: swaps an item with its neighbor, then numbers the whole list 0, 1, 2... */
final class Ordering {
    private Ordering() {}

    /** dir is "up" (earlier) or "down" (later); anything else just renumbers. */
    static <T> void move(List<T> list, T item, String dir, ObjIntConsumer<T> setOrder) {
        List<T> items = new ArrayList<>(list);
        int i = items.indexOf(item);
        int j = "up".equals(dir) ? i - 1 : "down".equals(dir) ? i + 1 : i;
        if (i >= 0 && j >= 0 && j < items.size()) {
            items.set(i, items.get(j));
            items.set(j, item);
        }
        for (int k = 0; k < items.size(); k++) setOrder.accept(items.get(k), k);
    }
}
