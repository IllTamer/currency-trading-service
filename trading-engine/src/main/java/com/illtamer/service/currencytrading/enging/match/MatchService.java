package com.illtamer.service.currencytrading.enging.match;

import com.illtamer.service.currencytrading.common.enums.DirectionEnum;
import com.illtamer.service.currencytrading.common.enums.OrderStatus;
import com.illtamer.service.currencytrading.common.model.trade.OrderEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 撮合服务
 * */
@Component
public class MatchService {

    private final OrderBook buyBook = new OrderBook(DirectionEnum.BUY);
    private final OrderBook sellBook = new OrderBook(DirectionEnum.SELL);
    /**
     * 最新市场价
     * */
    private BigDecimal marketPrice = BigDecimal.ZERO;
    /**
     * 上次处理的Sequence ID
     * */
    private long sequenceId;

    /**
     * @return 成交结果
     * */
    public MatchResult processOrder(long sequenceId, OrderEntity order) {
        this.sequenceId = sequenceId;
        return switch (order.getDirection()) {
            case BUY -> processOrder(order, sellBook, buyBook);
            case SELL -> processOrder(order, buyBook, sellBook);
        };
    }

    /**
     * 取消订单
     * */
    public void cancel(long timestamp, OrderEntity order) {
        OrderBook book = order.getDirection() == DirectionEnum.BUY ? buyBook : sellBook;
        if (!book.remove(order))
            throw new IllegalArgumentException("Order not found in order book");
        OrderStatus status = order.getUnfilledQuantity().compareTo(order.getQuantity()) == 0 ? OrderStatus.FULLY_CANCELLED : OrderStatus.PARTIAL_CANCELLED;
        order.updateOrder(order.getUnfilledQuantity(), status, timestamp);
    }

    /**
     * @param takerOrder 输入订单
     * @param makerBook 尝试匹配成交的 OrderBook
     * @param anotherBook 未能完全成交后挂单的 OrderBook
     * */
    private MatchResult processOrder(OrderEntity takerOrder, OrderBook makerBook, OrderBook anotherBook) {
        final long timestamp = takerOrder.getCreatedAt();
        MatchResult matchResult = new MatchResult(takerOrder);
        BigDecimal takerUnfilledQuantity = takerOrder.getQuantity();
        while (true) {
            final OrderEntity makerOrder = makerBook.getFirst();
            // 对手盘不存在
            if (makerOrder == null) break;
            final BigDecimal marketPrice = makerOrder.getPrice();
            // 买入价格低于卖盘第一档价格
            if (takerOrder.getDirection() == DirectionEnum.BUY && takerOrder.getPrice().compareTo(marketPrice) < 0) break;
            // 卖出价格高于买盘第一档价格
            if (takerOrder.getDirection() == DirectionEnum.SELL && takerOrder.getPrice().compareTo(marketPrice) > 0) break;
            this.marketPrice = makerOrder.getPrice();
            final BigDecimal matchedQuantity = takerUnfilledQuantity.min(makerOrder.getUnfilledQuantity());
            // record
            matchResult.add(makerOrder.getPrice(), matchedQuantity, makerOrder);
            // refresh
            takerUnfilledQuantity = takerUnfilledQuantity.subtract(matchedQuantity);
            final BigDecimal makerUnfilledQuantity = makerOrder.getUnfilledQuantity().subtract(matchedQuantity);
            if (makerUnfilledQuantity.signum() == 0) {
                // 对手盘全部成交
                makerOrder.updateOrder(makerUnfilledQuantity, OrderStatus.FULLY_FILLED, timestamp);
                makerBook.remove(makerOrder);
            } else {
                makerOrder.updateOrder(makerUnfilledQuantity, OrderStatus.PARTIAL_FILLED, timestamp);
            }
            // 余量为0
            if (takerUnfilledQuantity.signum() == 0) {
                takerOrder.updateOrder(takerUnfilledQuantity, OrderStatus.FULLY_FILLED, timestamp);
                break;
            }
        }
        // 仍有余量
        if (takerUnfilledQuantity.signum() > 0) {
            takerOrder.updateOrder(
                    takerUnfilledQuantity,
                    takerUnfilledQuantity.compareTo(takerOrder.getQuantity()) == 0 ?
                            OrderStatus.PENDING : OrderStatus.PARTIAL_FILLED,
                    timestamp
            );
            anotherBook.add(takerOrder);
        }
        return matchResult;
    }

    public OrderBook getBuyBook() {
        return buyBook;
    }

    public OrderBook getSellBook() {
        return sellBook;
    }

    public BigDecimal getMarketPrice() {
        return marketPrice;
    }

    public long getSequenceId() {
        return sequenceId;
    }

}
