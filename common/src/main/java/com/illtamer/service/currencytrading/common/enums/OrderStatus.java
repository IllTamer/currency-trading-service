package com.illtamer.service.currencytrading.common.enums;

/**
 * 订单状态枚举
 * */
public enum OrderStatus {

    /**
     * 等待成交 (unfilledQuantity == quantity)
     */
    PENDING(false),

    /**
     * 完全成交 (unfilledQuantity = 0)
     */
    FULLY_FILLED(true),

    /**
     * 部分成交 (quantity > unfilledQuantity > 0)
     */
    PARTIAL_FILLED(false),

    /**
     * 部分成交后取消 (quantity > unfilledQuantity > 0)
     */
    PARTIAL_CANCELLED(true),

    /**
     * 完全取消 (unfilledQuantity == quantity)
     */
    FULLY_CANCELLED(true);

    private final boolean finalStatus;

    OrderStatus(boolean finalStatus) {
        this.finalStatus = finalStatus;
    }

    public boolean isFinalStatus() {
        return finalStatus;
    }

}
