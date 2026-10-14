package com.madelmade.service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

/** The visitor's cart, kept for their browser session and carried over when they sign in. */
@Component
@SessionScope
public class Cart implements Serializable {

    private final List<CartLine> lines = new ArrayList<>();

    public List<CartLine> getLines() { return lines; }

    public boolean isEmpty() { return lines.isEmpty(); }

    public int getCount() {
        return lines.stream().mapToInt(CartLine::getQuantity).sum();
    }

    public int getLoafCount() {
        return lines.stream().filter(CartLine::isCountsAsLoaf).mapToInt(CartLine::getQuantity).sum();
    }

    public BigDecimal getTotal() {
        return lines.stream().map(CartLine::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isPriceTbd() {
        return lines.stream().anyMatch(CartLine::isPriceTbd);
    }

    public Optional<CartLine> find(String id) {
        return lines.stream().filter(l -> l.getId().equals(id)).findFirst();
    }

    public void add(CartLine line) { lines.add(line); }

    public void remove(String id) { lines.removeIf(l -> l.getId().equals(id)); }

    public void clear() { lines.clear(); }
}
