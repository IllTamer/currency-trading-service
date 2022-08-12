package com.illtamer.service.currencytrading.common.message.event;

import com.illtamer.service.currencytrading.common.enums.AssetEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Transfer between users
 * */
@Data
@EqualsAndHashCode(callSuper = true)
public class TransferEvent extends AbstractEvent {

    private Long fromUserId;

    private Long toUserId;

    private AssetEnum asset;

    private BigDecimal amount;

    /**
     * 是否充足
     * */
    private Boolean sufficient;

}
