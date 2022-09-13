package com.illtamer.service.currencytrading.common.messaging;

import com.illtamer.service.currencytrading.common.message.AbstractMessage;
import com.illtamer.service.currencytrading.common.util.JsonUtil;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Holds message types
 * */
@Component
public class MessageHolder {

    private static final Logger log = LoggerFactory.getLogger(MessageHolder.class);
    private static final char SEP = '#';

    private final Map<String, Class<? extends AbstractMessage>> messageTypes = new HashMap<>();

    public String serialize(AbstractMessage message) {
        final String type = message.getClass().getName();
        final String json = JsonUtil.writeJson(message);
        return type + SEP + json;
    }

    public List<AbstractMessage> deserialize(List<String> dataList) {
        List<AbstractMessage> list = new ArrayList<>(dataList.size());
        for (String data : dataList) {
            list.add(deserialize(data));
        }
        return list;
    }

    public List<AbstractMessage> deserializeConsumerRecords(List<ConsumerRecord<String, String>> dataList) {
        List<AbstractMessage> list = new ArrayList<>(dataList.size());
        for (ConsumerRecord<String, String> data : dataList) {
            list.add(deserialize(data.value()));
        }
        return list;
    }

    public AbstractMessage deserialize(String data) {
        int pos = data.indexOf(SEP);
        if (pos == -1) {
            throw new RuntimeException("Unable to handle message with data: " + data);
        }
        String type = data.substring(0, pos);
        Class<? extends AbstractMessage> clazz = messageTypes.get(type);
        if (clazz == null) {
            throw new RuntimeException("Unable to handle message with type: " + type);
        }
        String json = data.substring(pos + 1);
        return JsonUtil.readJson(json, clazz);
    }

    @SuppressWarnings("unchecked")
    @PostConstruct
    public void init() {
        log.debug("Find message classes ...");
        final ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(((metadataReader, metadataReaderFactory) -> {
            final String className = metadataReader.getClassMetadata().getClassName();
            try {
                final Class<?> clazz = Class.forName(className);
                return AbstractMessage.class.isAssignableFrom(clazz);
            } catch (ClassNotFoundException e) {
                log.warn("Can't find class {}", className);
                throw new RuntimeException(e);
            }
        }));
        final Set<BeanDefinition> beans = provider.findCandidateComponents(AbstractMessage.class.getPackageName());
        for (BeanDefinition bean : beans) {
            try {
                final Class<?> clazz = Class.forName(bean.getBeanClassName());
                log.debug("Found message class: {}", clazz.getName());
                if (messageTypes.put(clazz.getName(), (Class<? extends AbstractMessage>) clazz) != null)
                    throw new RuntimeException("Duplicate message class name: " + clazz.getName());
            } catch (ClassNotFoundException e) {
                log.warn("Can't find class {}", bean.getBeanClassName(), e);
            }
        }
    }

}
