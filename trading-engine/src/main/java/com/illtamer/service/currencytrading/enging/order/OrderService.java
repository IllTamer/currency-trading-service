package com.illtamer.service.currencytrading.enging.order;

import com.illtamer.service.currencytrading.common.enums.AssetEnum;
import com.illtamer.service.currencytrading.common.enums.DirectionEnum;
import com.illtamer.service.currencytrading.common.model.trade.OrderEntity;
import com.illtamer.service.currencytrading.enging.asset.AssetService;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderService {

    /**
     * 跟踪所有活动订单: Order ID => OrderEntity
     * */
    private final Map<Long, OrderEntity> activeOrders = new ConcurrentHashMap<>();
    /**
     * 跟踪用户活动订单: User ID => Map(Order ID => OrderEntity)
     * */
    private final Map<Long, Map<Long, OrderEntity>> userOrders = new ConcurrentHashMap<>();

    private final AssetService assetService;

    @Autowired
    public OrderService(AssetService assetService) {
        this.assetService = assetService;
    }

    /**
     * 创建订单
     * @param price BTC 价格
     * @param quantity BTC 数量
     * */
    @Nullable
    public OrderEntity createOrder(long sequenceId, long timestamp, Long orderId, Long userId,
                                   DirectionEnum direction, BigDecimal price, BigDecimal quantity) {
        switch (direction) {
            case BUY -> {
                // 买入 冻结usd数量 = btc数量 * btc价格
                if (!assetService.tryFreeze(userId, AssetEnum.USD, price.multiply(quantity)))
                    return null;
            }
            case SELL -> {
                // 卖出 冻结btc数量
                if (!assetService.tryFreeze(userId, AssetEnum.BTC, quantity))
                    return null;
            }
        }
        OrderEntity order = OrderEntity.builder()
                .id(orderId)
                .sequenceId(sequenceId)
                .userId(userId)
                .direction(direction)
                .price(price)
                .quantity(quantity)
                .unfilledQuantity(quantity)
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .build();
        activeOrders.put(order.getId(), order);
        userOrders.computeIfAbsent(order.getUserId(), key -> new ConcurrentHashMap<>())
                .put(order.getId(), order);
        return order;
    }

    /**
     * 删除订单
     * */
    public void removeOrder(Long orderId) {
        final OrderEntity removed = activeOrders.remove(orderId);
        Assert.notNull(removed, "Order not found by orderId in active orders: " + orderId);
        final Map<Long, OrderEntity> userOrders = this.userOrders.get(removed.getUserId());
        Assert.notNull(userOrders, "User orders not found by userId: " + removed.getUserId());
        final OrderEntity removedUserOrder = userOrders.remove(orderId);
        Assert.notNull(removedUserOrder, "Order not found by orderId in user orders: " + orderId);
    }

    /**
     * 根据订单ID查询活动Order
     * */
    @Nullable
    public OrderEntity getOrder(Long orderId) {
        return activeOrders.get(orderId);
    }

    /**
     * 根据用户ID查询用户所有活动Order
     * */
    @Nullable
    public Map<Long, OrderEntity> getUserOrders(Long userId) {
        return userOrders.get(userId);
    }

}
