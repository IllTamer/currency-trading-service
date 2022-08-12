package com.illtamer.service.currencytrading.enging.match;

import com.illtamer.service.currencytrading.common.model.trade.OrderEntity;

import java.math.BigDecimal;

public record MatchDetailRecord(
        BigDecimal price,
        BigDecimal quantity,
        OrderEntity takerOrder,
        OrderEntity makerOrder
) {
}
