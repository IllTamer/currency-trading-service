package com.illtamer.service.currencytrading.api.web;

import com.illtamer.service.currencytrading.api.TradingEngineApiProxyService;
import com.illtamer.service.currencytrading.common.bean.OrderBookBean;
import com.illtamer.service.currencytrading.common.bean.OrderRequestBean;
import com.illtamer.service.currencytrading.common.context.UserContext;
import com.illtamer.service.currencytrading.common.message.event.OrderRequestEvent;
import com.illtamer.service.currencytrading.common.redis.RedisCache;
import com.illtamer.service.currencytrading.common.redis.RedisService;
import com.illtamer.service.currencytrading.common.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class TradingAPIController {

    // 消息refId -> DeferredResult:
    private final Map<String, DeferredResult<ResponseEntity<String>>> deferredResultMap = new ConcurrentHashMap<>();

    private final TradingEngineApiProxyService proxyService;
    private final RedisService redisService;

    @Autowired
    public TradingAPIController(
            TradingEngineApiProxyService proxyService,
            RedisService redisService
    ) {
        this.proxyService = proxyService;
        this.redisService = redisService;
    }

    @PostConstruct
    public void init() {
        // 订阅 Redis
        redisService.subscribe(RedisCache.Topic.TRADING_API_RESULT, this::onApiResultMessage);
    }

    /**
     * 创建订单
     * @apiNote 已认证用户
     * */
    @PostMapping(value = "/orders", produces = "application/json")
    @ResponseBody
    public DeferredResult<ResponseEntity<String>> createOrder(@RequestBody OrderRequestBean orderRequest) {
        final Long userId = UserContext.getRequiredUserId();
        final String refId = IdUtil.generateUniqueId();
        final OrderRequestEvent event = new OrderRequestEvent();
        event.setRefId(refId);
        event.setUserId(userId);
        event.setDirection(orderRequest.getDirection());
        event.setPrice(orderRequest.price);
        event.setQuality(orderRequest.quantity);
        event.setCreatedAt(System.currentTimeMillis());
        // 超时返回
        ResponseEntity<String> timeout = new ResponseEntity<>(getTimeoutJson(), HttpStatus.BAD_REQUEST);
        // 正常异步返回
        DeferredResult<ResponseEntity<String>> deferred = new DeferredResult<>(500, timeout); // 0.5秒超时
        deferred.onTimeout(() -> this.deferredResultMap.remove(event.getRefId()));
        // 根据refId跟踪消息处理结果:
        this.deferredResultMap.put(event.getRefId(), deferred);
        // 发送消息:
        sendMessage(event);
        return deferred;
    }

    /**
     * 获取用户资产
     * @apiNote 已认证用户
     * */
    @ResponseBody
    @GetMapping(value = "/assets", produces = "application/json")
    public String getAssets() throws IOException {
        return proxyService.get("/internal/" + UserContext.getRequiredUserId() + "/assets");
    }

    /**
     * 获取公开市场的订单簿
     * @return Redis 缓存结果
     * */
    @ResponseBody
    @GetMapping(value = "orderBook", produces = "application/json")
    public String getOrderBook() {
        String data = redisService.get(RedisCache.Key.ORDER_BOOK);
        return data == null ? OrderBookBean.EMPTY : data;
    }

    // 收到Redis的消息结果推送:
    public void onApiResultMessage(String msg) {
        ApiResultMessage message = objectMapper.readValue(msg, ApiResultMessage.class);
        if (message.refId != null) {
            // 根据消息refId查找DeferredResult:
            DeferredResult<ResponseEntity<String>> deferred = this.deferredResultMap.remove(message.refId);
            if (deferred != null) {
                // 找到DeferredResult后设置响应结果:
                ResponseEntity<String> resp = new ResponseEntity<>(JsonUtil.writeJson(message.result), HttpStatus.OK);
                deferred.setResult(resp);
            }
        }
    }

}
