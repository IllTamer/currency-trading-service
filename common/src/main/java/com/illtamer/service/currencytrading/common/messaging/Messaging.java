package com.illtamer.service.currencytrading.common.messaging;

import lombok.Getter;

/**
 * 消息传递枚举
 * */
public interface Messaging {

    @Getter
    enum Topic {

        /**
         * Topic name: to sequence.
         */
        SEQUENCE(1),

        /**
         * Topic name: to/from trading-engine.
         */
        TRANSFER(1),

        /**
         * Topic name: events to trading-engine.
         */
        TRADE(1),

        /**
         * Topic name: tick to quotation for generate bars.
         */
        TICK(1);

        private final int concurrency;

        Topic(int concurrency) {
            this.concurrency = concurrency;
        }

    }

}
