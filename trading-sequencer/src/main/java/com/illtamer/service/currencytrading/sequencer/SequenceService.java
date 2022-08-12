package com.illtamer.service.currencytrading.sequencer;

import com.illtamer.service.currencytrading.common.message.MessageHolder;
import com.illtamer.service.currencytrading.common.message.event.AbstractEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sequence events
 * */
@Service
public class SequenceService {

    private static final Logger log = LoggerFactory.getLogger(SequenceService.class);

    private final SequenceHandler sequenceHandler;
    private final MessageHolder messageHolder;

    /**
     * 全局唯一递增 ID
     * */
    private AtomicLong sequence;

    @Autowired
    public SequenceService(SequenceHandler sequenceHandler, MessageHolder messageHolder) {
        this.sequenceHandler = sequenceHandler;
        this.messageHolder = messageHolder;
    }

    /**
     * @apiNote 接收消息定序后再发送
     * */
    synchronized void processMessages(List<AbstractEvent> messages) {
        List<AbstractEvent> sequenced;
        try {
            sequenced = sequenceHandler.sequenceMessages(messageHolder, sequence, messages);
        } catch (Throwable e) {
            log.error("Exception occurred when doing sequence", e);
            System.exit(1);
            throw new Error(e);
        }
        sendMessages(sequenced);
    }

}
