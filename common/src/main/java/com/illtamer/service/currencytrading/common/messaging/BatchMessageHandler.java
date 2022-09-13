package com.illtamer.service.currencytrading.common.messaging;

import com.illtamer.service.currencytrading.common.message.AbstractMessage;

import java.util.List;

@FunctionalInterface
public interface BatchMessageHandler<T extends AbstractMessage> {

    void processMessages(List<T> messages);

}
