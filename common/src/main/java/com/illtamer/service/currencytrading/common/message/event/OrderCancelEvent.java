package com.illtamer.service.currencytrading.common.message.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderCancelEvent extends AbstractEvent {

    private Long userId;

    private Long refOrderId;

}
