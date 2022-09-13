package com.illtamer.service.currencytrading.enging.service;

import com.illtamer.service.currencytrading.common.message.event.AbstractEvent;
import com.illtamer.service.currencytrading.common.message.event.OrderCancelEvent;
import com.illtamer.service.currencytrading.common.message.event.OrderRequestEvent;
import com.illtamer.service.currencytrading.common.message.event.TransferEvent;
import com.illtamer.service.currencytrading.common.model.trade.OrderEntity;
import com.illtamer.service.currencytrading.enging.asset.AssetService;
import com.illtamer.service.currencytrading.enging.asset.TransferEnum;
import com.illtamer.service.currencytrading.enging.clearing.ClearingService;
import com.illtamer.service.currencytrading.enging.match.MatchDetailRecord;
import com.illtamer.service.currencytrading.enging.match.MatchResult;
import com.illtamer.service.currencytrading.enging.match.MatchService;
import com.illtamer.service.currencytrading.enging.order.OrderService;
import com.illtamer.service.currencytrading.enging.store.StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 交易引擎服务
 * <p>
 * 由事件驱动的状态机模型,同样的输入将得到同样的输出
 * */
@Service
public class TradingEngineService {

    private static final Logger log = LoggerFactory.getLogger(TradingEngineService.class);

    private final Queue<List<OrderEntity>> orderQueue = new ConcurrentLinkedDeque<>();
    private final ZoneId zoneId = ZoneId.systemDefault();

    private final AssetService assetService;
    private final OrderService orderService;
    private final MatchService matchService;
    private final ClearingService clearingService;
    private final StoreService storeService;

    private long lastSequenceId = 0;
    private boolean fatalError = false;
    private boolean orderBookChanged = false;

    @Autowired
    public TradingEngineService(AssetService assetService, OrderService orderService, MatchService matchService, ClearingService clearingService, StoreService storeService) {
        this.assetService = assetService;
        this.orderService = orderService;
        this.matchService = matchService;
        this.clearingService = clearingService;
        this.storeService = storeService;
    }

    public void processMessages(List<AbstractEvent> messages) {
        for (AbstractEvent message : messages) {
            processEvent(message);
        }
    }

    protected void processEvent(AbstractEvent e) {
        if (e.getSequenceId() <= lastSequenceId) {
            log.warn("Skip duplicate event: {}", e);
            return;
        }
        // 是否丢失消息
        if (e.getPreviousId() > lastSequenceId) {
            List<AbstractEvent> events = storeService.loadEventsFromDB(lastSequenceId);
            if (events.isEmpty()) {
                System.exit(1);
                return;
            }
            for (AbstractEvent event : events) {
                processEvent(event);
            }
            return;
        }
        // 当前消息是否指向上一条 / 是否连续
        if (e.getPreviousId() != lastSequenceId) {
            System.exit(1);
            return;
        }
        try {
            if (e instanceof OrderRequestEvent event) {
                createOrder(event);
            } else if (e instanceof OrderCancelEvent event) {
                cancelOrder(event);
            } else if (e instanceof TransferEvent event) {
                transfer(event);
            }
        } catch (Exception exception) {
            log.error("Process event error", exception);
            panic();
            return;
        }
        lastSequenceId = e.getSequenceId();
    }

    private void createOrder(OrderRequestEvent event) {
        final ZonedDateTime dateTime = Instant.ofEpochMilli(event.getCreatedAt()).atZone(zoneId);
        long orderId = event.getSequenceId() * 10000 + (dateTime.getYear() * 100L + dateTime.getMonth().getValue());
        final OrderEntity order = orderService.createOrder(event.getSequenceId(), event.getCreatedAt(), orderId, event.getUserId(), event.getDirection(), event.getPrice(), event.getQuality());
        if (order == null) {
            log.warn("Failed to create order");
            return;
        }
        final MatchResult result = matchService.processOrder(event.getSequenceId(), order);
        clearingService.clearMatchResult(result);
        if (result.getMatchDetails().isEmpty()) return;
        // 清算后收集已完成 order
        List<OrderEntity> closedOrders = new ArrayList<>();
        if (result.getTakerOrder().getStatus().isFinalStatus()) {
            closedOrders.add(result.getTakerOrder());
        }
        for (MatchDetailRecord detail : result.getMatchDetails()) {
            final OrderEntity maker = detail.makerOrder();
            if (maker.getStatus().isFinalStatus()) {
                closedOrders.add(maker);
            }
        }
        orderQueue.add(closedOrders);
    }

    private void cancelOrder(OrderCancelEvent event) {
        final OrderEntity order = orderService.getOrder(event.getRefOrderId());
        if (order == null || !order.getUserId().equals(event.getUserId())) {
            return;
        }
        matchService.cancel(event.getCreatedAt(), order);
        clearingService.clearCancelOrder(order);
        orderBookChanged = true;
    }

    private boolean transfer(TransferEvent event) {
        return assetService.tryTransfer(TransferEnum.AVAILABLE_TO_AVAILABLE, event.getFromUserId(), event.getToUserId(), event.getAsset(), event.getAmount(), event.getSufficient());
    }

    private void panic() {
        log.error("Application panic, exit now ...");
        this.fatalError = true;
        System.exit(1);
    }

}
