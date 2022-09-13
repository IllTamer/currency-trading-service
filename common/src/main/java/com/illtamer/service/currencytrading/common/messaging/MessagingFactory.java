package com.illtamer.service.currencytrading.common.messaging;

import com.illtamer.service.currencytrading.common.message.AbstractMessage;
import com.illtamer.service.currencytrading.common.message.event.AbstractEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpoint;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.BatchMessageListener;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.TopicPartitionOffset;
import org.springframework.kafka.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class MessagingFactory {

    private static final Logger log = LoggerFactory.getLogger(MessagingFactory.class);

    private final MessageHolder messageHolder;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ConcurrentKafkaListenerContainerFactory<String, String> listenerContainerFactory;
    private final KafkaAdmin kafkaAdmin;

    public MessagingFactory(
            MessageHolder messageHolder,
            KafkaTemplate<String, String> kafkaTemplate,
            ConcurrentKafkaListenerContainerFactory<String, String> listenerContainerFactory,
            KafkaAdmin kafkaAdmin
    ) {
        this.messageHolder = messageHolder;
        this.kafkaTemplate = kafkaTemplate;
        this.listenerContainerFactory = listenerContainerFactory;
        this.kafkaAdmin = kafkaAdmin;
    }

    public <T extends AbstractEvent> MessageProducer<T> createMessageProducer(Messaging.Topic topic, Class<AbstractEvent> messageClass) {
        log.debug("Try create message producer for topic {}...", topic);
        final String name = topic.name();
        return message -> kafkaTemplate.send(name, messageHolder.serialize(message));
    }

    public <T extends AbstractMessage> MessageConsumer createBatchMessageListener(Messaging.Topic topic, String groupId,
                                                                                  BatchMessageHandler<T> messageHandler) {
        return createBatchMessageListener(topic, groupId, messageHandler, null);
    }

    public <T extends AbstractMessage> MessageConsumer createBatchMessageListener(Messaging.Topic topic, String groupId,
                                                                                  BatchMessageHandler<T> messageHandler, CommonErrorHandler errorHandler) {
        log.info("Try create batch message listener for topic {}: group id = {}...", topic, groupId);
        ConcurrentMessageListenerContainer<String, String> listenerContainer = listenerContainerFactory
                .createListenerContainer(new KafkaListenerEndpointAdapter() {
                    @Override
                    public String getGroupId() {
                        return groupId;
                    }
                    @Override
                    public Collection<String> getTopics() {
                        return List.of(topic.name());
                    }
                });
        listenerContainer.setupMessageListener((BatchMessageListener<String, String>) data -> {
            List<T> messages = new ArrayList<>(data.size());
            for (ConsumerRecord<String, String> record : data) {
                AbstractMessage message = messageHolder.deserialize(record.value());
                messages.add((T) message);
            }
            messageHandler.processMessages(messages);
        });
        if (errorHandler != null) {
            listenerContainer.setCommonErrorHandler(errorHandler);
        }
        listenerContainer.start();
        return listenerContainer::stop;
    }

    private static class KafkaListenerEndpointAdapter implements KafkaListenerEndpoint {

        @Override
        public String getId() {
            return null;
        }

        @Override
        public String getGroupId() {
            return null;
        }

        @Override
        public String getGroup() {
            return null;
        }

        @Override
        public Collection<String> getTopics() {
            return List.of();
        }

        @Override
        public Pattern getTopicPattern() {
            return null;
        }

        @Override
        public String getClientIdPrefix() {
            return null;
        }

        @Override
        public Integer getConcurrency() {
            return Integer.valueOf(1);
        }

        @Override
        public Boolean getAutoStartup() {
            return Boolean.FALSE;
        }

        @Override
        public void setupListenerContainer(MessageListenerContainer listenerContainer, MessageConverter messageConverter) {
        }

        @Override
        public TopicPartitionOffset[] getTopicPartitionsToAssign() {
            return null;
        }

        @Override
        public boolean isSplitIterables() {
            return false;
        }

    }

}
