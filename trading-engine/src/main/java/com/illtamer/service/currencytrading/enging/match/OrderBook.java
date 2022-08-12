package com.illtamer.service.currencytrading.enging.match;

import com.illtamer.service.currencytrading.common.enums.DirectionEnum;
import com.illtamer.service.currencytrading.common.model.trade.OrderEntity;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 订单簿
 * */
public class OrderBook {

    /**
     * 卖出排序
     * <p>
     * 价格低、时间早
     * */
    private static final Comparator<OrderKeyRecord> SORT_SELL = Comparator
            .comparing(OrderKeyRecord::price)
            .thenComparingLong(OrderKeyRecord::sequenceId);
    /**
     * 买入出排序
     * <p>
     * 价格高、时间早
     * */
    private static final Comparator<OrderKeyRecord> SORT_BUY = (o1, o2) -> {
        int cmp = o2.price().compareTo(o1.price());
        return cmp == 0 ? Long.compare(o1.sequenceId(), o2.sequenceId()) : cmp;
    };

    /**
     * 方向
     * */
    private final DirectionEnum direction;
    /**
     * 红黑树
     * */
    private final TreeMap<OrderKeyRecord, OrderEntity> book;

    public OrderBook(DirectionEnum direction) {
        this.direction = direction;
        this.book = new TreeMap<>(direction == DirectionEnum.BUY ? SORT_BUY : SORT_SELL);
    }

    public boolean add(OrderEntity order) {
        return book.put(new OrderKeyRecord(order.getSequenceId(), order.getPrice()), order) == null;
    }

    public boolean remove(OrderEntity order) {
        return book.remove(new OrderKeyRecord(order.getSequenceId(), order.getPrice())) != null;
    }

    @Nullable
    public OrderEntity getFirst() {
        return book.isEmpty() ? null : book.firstEntry().getValue();
    }

    @Override
    public String toString() {
        if (this.book.isEmpty()) {
            return "(empty)";
        }
        List<String> orders = new ArrayList<>(8);
        for (Map.Entry<OrderKeyRecord, OrderEntity> entry : this.book.entrySet()) {
            OrderEntity order = entry.getValue();
            orders.add("  " + order.getPrice() + " " + order.getUnfilledQuantity() + " " + order);
        }
        if (direction == DirectionEnum.SELL) {
            Collections.reverse(orders);
        }
        return String.join("\n", orders);
    }

}
