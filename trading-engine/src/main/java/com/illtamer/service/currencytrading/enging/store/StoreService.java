package com.illtamer.service.currencytrading.enging.store;

import com.illtamer.service.currencytrading.common.database.DBTemplate;
import com.illtamer.service.currencytrading.common.messaging.MessageHolder;
import com.illtamer.service.currencytrading.common.message.event.AbstractEvent;
import com.illtamer.service.currencytrading.common.model.trade.EventEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class StoreService {

    private final MessageHolder messageHolder;
    private final DBTemplate dbTemplate;

    @Autowired
    public StoreService(MessageHolder messageHolder, DBTemplate dbTemplate) {
        this.messageHolder = messageHolder;
        this.dbTemplate = dbTemplate;
    }

    public List<AbstractEvent> loadEventsFromDB(long lastEventId) {
        List<EventEntity> events = this.dbTemplate.from(EventEntity.class)
                .where("sequenceId > ?", lastEventId)
                .orderBy("sequenceId")
                .limit(100000).list();
        return events.stream()
                .map(event -> (AbstractEvent) messageHolder.deserialize(event.getData()))
                .collect(Collectors.toList());
    }

}
