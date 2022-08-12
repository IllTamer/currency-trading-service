package com.illtamer.service.currencytrading.common.enums;

/**
 * 订单方向枚举
 * */
public enum DirectionEnum {

    SELL(0),

    BUY(1);

    /**
     * Get negate direction.
     */
    public DirectionEnum negate() {
        return this == BUY ? SELL : BUY;
    }

    private final int value;

    DirectionEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static DirectionEnum of(int intValue) {
        return switch (intValue) {
            case 0 -> SELL;
            case 1 -> BUY;
            default -> throw new IllegalArgumentException("Invalid Direction value");
        };
    }

}
