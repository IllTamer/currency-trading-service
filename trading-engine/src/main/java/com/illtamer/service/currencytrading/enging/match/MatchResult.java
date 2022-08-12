package com.illtamer.service.currencytrading.enging.match;

import com.illtamer.service.currencytrading.common.model.trade.OrderEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 撮合结果实体类
 * */
public class MatchResult {

    private final OrderEntity takerOrder;
    private final List<MatchDetailRecord> matchDetails = new ArrayList<>();

    public MatchResult(OrderEntity takerOrder) {
        this.takerOrder = takerOrder;
    }

    public void add(BigDecimal price, BigDecimal matchedQuantity, OrderEntity makerOrder) {
        matchDetails.add(new MatchDetailRecord(price, matchedQuantity, takerOrder, makerOrder));
    }

    public OrderEntity getTakerOrder() {
        return takerOrder;
    }

    public List<MatchDetailRecord> getMatchDetails() {
        return matchDetails;
    }

    @Override
    public String toString() {
        if (matchDetails.isEmpty()) {
            return "no matched.";
        }
        return matchDetails.size() + " matched: "
                + String.join(", ", matchDetails.stream().map(MatchDetailRecord::toString).toArray(String[]::new));
    }

}
