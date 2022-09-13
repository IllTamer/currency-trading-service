package com.illtamer.service.currencytrading.common.message;

import lombok.Data;

import java.io.Serializable;

/**
 * Base message object
 * */
@Data
public abstract class AbstractMessage implements Serializable {

    /**
     * 消息的 Reference ID
     * */
    private String refId;

    private long createdAt;

}
