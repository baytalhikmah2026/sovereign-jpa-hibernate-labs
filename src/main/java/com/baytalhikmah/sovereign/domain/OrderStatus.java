package com.baytalhikmah.sovereign.domain;

import java.util.Arrays;
import java.util.Set;

public enum OrderStatus {

    NEW("NW"),
    PAID("PD"),
    SHIPPED("SH"),
    DELIVERED("DL"),
    CANCELLED("CX");

    private final String code;

    OrderStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static OrderStatus fromCode(String code) {
        return Arrays.stream(values())
            .filter(status -> status.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown order status code: " + code));
    }

    public boolean canMoveTo(OrderStatus next) {
        return switch (this) {
            case NEW -> Set.of(PAID, CANCELLED).contains(next);
            case PAID -> Set.of(SHIPPED, CANCELLED).contains(next);
            case SHIPPED -> next == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }

}
