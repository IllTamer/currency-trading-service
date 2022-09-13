package com.illtamer.service.currencytrading.sequencer;

import com.illtamer.service.currencytrading.common.messaging.*;
import com.illtamer.service.currencytrading.common.message.event.AbstractEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sequence events
 * */
@Service
public class SequenceService implements CommonErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(SequenceService.class);
    private static final String GROUP_ID = "SequencerGroup";

    private final SequenceHandler sequenceHandler;
    private final MessageHolder messageHolder;
    private final MessagingFactory messagingFactory;

    private MessageProducer<AbstractEvent> messageProducer;
    private MessageConsumer consumer;
    private boolean running;
    private boolean crash;

    /**
     * 全局唯一递增 ID
     * */
    private AtomicLong sequence;

    @Autowired
    public SequenceService(
            SequenceHandler sequenceHandler,
            MessageHolder messageHolder,
            MessagingFactory messagingFactory
    ) {
        this.sequenceHandler = sequenceHandler;
        this.messageHolder = messageHolder;
        this.messagingFactory = messagingFactory;
    }

    @PostConstruct
    public void init() {
        log.debug("Start sequence job ...");
        this.messageProducer = messagingFactory.createMessageProducer(Messaging.Topic.TRADE, AbstractEvent.class);
        log.info("Create message consumer for {} ...", getClass().getName());
        this.consumer = messagingFactory.createBatchMessageListener(Messaging.Topic.SEQUENCE,
                GROUP_ID, this::processMessages, this);
        this.running = true;
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        log.info("Close message consumer for {} ...", getClass().getName());
        consumer.stop();
    }

    /**
     * Message consumer error handler
     */
    @Override
    public void handleBatch(@NotNull Exception thrownException, @NotNull ConsumerRecords<?, ?> data, @NotNull Consumer<?, ?> consumer,
                            @NotNull MessageListenerContainer container, @NotNull Runnable invokeListener) {
        log.error("batch error!", thrownException);
        panic();
    }

    /**
     * 接收消息定序后再发送
     * */
    synchronized void processMessages(List<AbstractEvent> messages) {
        if (!running || crash) {
            panic();
            return;
        }
        List<AbstractEvent> sequenced;
        try {
            sequenced = sequenceHandler.sequenceMessages(messageHolder, sequence, messages);
        } catch (Throwable e) {
            log.error("Exception occurred when doing sequence", e);
            System.exit(1);
            throw new Error(e);
        }
        // 发送定序后的消息:
        messageProducer.sendMessage(sequenced);
    }

    private void panic() {
        this.crash = true;
        this.running = false;
        System.exit(1);
    }

}
