package com.illtamer.service.currencytrading.common.messaging;

import com.illtamer.service.currencytrading.common.message.AbstractMessage;

import java.util.List;

@FunctionalInterface
public interface MessageProducer<T extends AbstractMessage> {

    void sendMessage(T message);

    default void sendMessage(List<T> messages) {
        messages.forEach(this::sendMessage);
    }

}
