package com.illtamer.service.currencytrading.common.message;

import com.illtamer.service.currencytrading.common.exception.APIError;
import com.illtamer.service.currencytrading.common.exception.APIErrorResponse;
import com.illtamer.service.currencytrading.common.model.trade.OrderEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API result message.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class APIResultMessage extends AbstractMessage {

    private APIErrorResponse error;

    private Object result;


    private static final APIErrorResponse CREATE_ORDER_FAILED = new APIErrorResponse(APIError.NO_ENOUGH_ASSET, null,
            "No enough available asset");

    private static final APIErrorResponse CANCEL_ORDER_FAILED = new APIErrorResponse(APIError.ORDER_NOT_FOUND, null,
            "Order not found");

    public static APIResultMessage createOrderFailed(String refId, long ts) {
        APIResultMessage msg = new APIResultMessage();
        msg.error = CREATE_ORDER_FAILED;
        msg.refId = refId;
        msg.createdAt = ts;
        return msg;
    }

    public static APIResultMessage cancelOrderFailed(String refId, long ts) {
        APIResultMessage msg = new APIResultMessage();
        msg.error = CANCEL_ORDER_FAILED;
        msg.refId = refId;
        msg.createdAt = ts;
        return msg;
    }

    public static APIResultMessage orderSuccess(String refId, OrderEntity order, long ts) {
        APIResultMessage msg = new APIResultMessage();
        msg.result = order;
        msg.refId = refId;
        msg.createdAt = ts;
        return msg;
    }

}
