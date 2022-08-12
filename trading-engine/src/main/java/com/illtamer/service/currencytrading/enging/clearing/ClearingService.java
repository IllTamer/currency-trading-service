package com.illtamer.service.currencytrading.enging.clearing;

import com.illtamer.service.currencytrading.common.enums.AssetEnum;
import com.illtamer.service.currencytrading.common.model.trade.OrderEntity;
import com.illtamer.service.currencytrading.enging.asset.AssetService;
import com.illtamer.service.currencytrading.enging.asset.TransferEnum;
import com.illtamer.service.currencytrading.enging.match.MatchDetailRecord;
import com.illtamer.service.currencytrading.enging.match.MatchResult;
import com.illtamer.service.currencytrading.enging.order.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 清算系统
 * <p>
 * 处理撮合结果，完成资产交换，清算系统本身没有状态
 * */
@Service
public class ClearingService {

    private final AssetService assetService;
    private final OrderService orderService;

    @Autowired
    public ClearingService(AssetService assetService, OrderService orderService) {
        this.assetService = assetService;
        this.orderService = orderService;
    }

    /**
     * 处理撮合结果
     * */
    public void clearMatchResult(MatchResult result) {
        final OrderEntity taker = result.getTakerOrder();
        switch (taker.getDirection()) {
            case BUY -> {
                // 买入成功时 taker#price >= maker#price
                for (MatchDetailRecord detail : result.getMatchDetails()) {
                    final OrderEntity maker = detail.makerOrder();
                    final BigDecimal matched = detail.quantity();
                    // 实际买入价低于报价，退钱
                    if (taker.getPrice().compareTo(maker.getPrice()) > 0) {
                        final BigDecimal unfreezeUSD = taker.getPrice().subtract(maker.getPrice()).multiply(matched);
                        assetService.unfreeze(taker.getUserId(), AssetEnum.USD, unfreezeUSD);
                    }
                    // 买方 USD -> 卖方
                    assetService.transfer(TransferEnum.FROZEN_TO_AVAILABLE, taker.getUserId(), maker.getUserId(), AssetEnum.USD, maker.getPrice().multiply(matched));
                    // 卖方 BTC -> 买方
                    assetService.transfer(TransferEnum.FROZEN_TO_AVAILABLE, maker.getUserId(), taker.getUserId(), AssetEnum.BTC, matched);
                    if (maker.getUnfilledQuantity().signum() == 0)
                        orderService.removeOrder(maker.getId());
                }
                if (taker.getUnfilledQuantity().signum() == 0)
                    orderService.removeOrder(taker.getId());
            }
            case SELL -> {
                for (MatchDetailRecord detail : result.getMatchDetails()) {
                    final OrderEntity maker = detail.makerOrder();
                    final BigDecimal matched = detail.quantity();
                    // 卖方 BTC -> 买方
                    assetService.transfer(TransferEnum.FROZEN_TO_AVAILABLE, taker.getUserId(), maker.getUserId(), AssetEnum.BTC, matched);
                    // 买方 USD -> 卖方
                    assetService.transfer(TransferEnum.FROZEN_TO_AVAILABLE, maker.getUserId(), taker.getUserId(), AssetEnum.USD, maker.getPrice().multiply(matched));
                    if (maker.getUnfilledQuantity().signum() == 0)
                        orderService.removeOrder(maker.getId());
                }
                if (taker.getUnfilledQuantity().signum() == 0)
                    orderService.removeOrder(taker.getId());
            }
        }
    }

    /**
     * 处理取消订单
     * */
    public void clearCancelOrder(OrderEntity order) {
        switch (order.getDirection()) {
            case BUY -> {
                // 解冻 USD
                assetService.unfreeze(order.getUserId(), AssetEnum.USD, order.getPrice().multiply(order.getUnfilledQuantity()));
            }
            case SELL -> {
                // 解冻 BTC
                assetService.unfreeze(order.getUserId(), AssetEnum.BTC, order.getUnfilledQuantity());
            }
        }
        orderService.removeOrder(order.getId());
    }

}
