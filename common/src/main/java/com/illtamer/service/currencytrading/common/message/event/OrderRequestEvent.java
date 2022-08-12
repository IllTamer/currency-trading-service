package com.illtamer.service.currencytrading.common.message.event;

import com.illtamer.service.currencytrading.common.enums.DirectionEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderRequestEvent extends AbstractEvent {

    private Long userId;

    private DirectionEnum direction;

    private BigDecimal price;

    private BigDecimal quality;

}
