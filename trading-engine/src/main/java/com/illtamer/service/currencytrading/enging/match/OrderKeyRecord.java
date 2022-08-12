package com.illtamer.service.currencytrading.enging.match;

import java.math.BigDecimal;

/**
 * 订单簿键值
 * <p>
 * 排序规则见 {@link OrderBook}
 * */
public record OrderKeyRecord(
        long sequenceId,
        BigDecimal price
) {

}
