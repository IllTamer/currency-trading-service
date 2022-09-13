package com.illtamer.service.currencytrading.sequencer;

import com.illtamer.service.currencytrading.common.messaging.MessageHolder;
import com.illtamer.service.currencytrading.common.message.event.AbstractEvent;
import com.illtamer.service.currencytrading.common.model.trade.EventEntity;
import com.illtamer.service.currencytrading.common.model.trade.UniqueEventEntity;
import com.illtamer.service.currencytrading.common.support.AbstractDBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Transactional(rollbackFor = Throwable.class)
public class SequenceHandler extends AbstractDBService {

    private static final Logger log = LoggerFactory.getLogger(SequenceHandler.class);

    private long lastTimestamp;

    public List<AbstractEvent> sequenceMessages(MessageHolder messageHolder, AtomicLong sequence, List<AbstractEvent> messages) throws Exception {
        List<UniqueEventEntity> uniques = new ArrayList<>(messages.size());
        Set<String> uniqueKeys = new HashSet<>(messages.size());
        List<AbstractEvent> sequencedMessages = new ArrayList<>(messages.size());
        List<EventEntity> events = new ArrayList<>(messages.size());
        for (AbstractEvent message : messages) {
            UniqueEventEntity unique = null;
            final String uniqueId = message.getUniqueId();
            // 在数据库中查找 uniqueId 是否存在
            if (uniqueId != null) {
                if (uniqueKeys.contains(uniqueId) || db.fetch(UniqueEventEntity.class, uniqueId) != null) {
                    log.warn("Ignore processed unique message: {}", message);
                    continue;
                }
                unique = new UniqueEventEntity();
                unique.setUniqueId(uniqueId);
                uniques.add(unique);
                uniqueKeys.add(uniqueId);
            }
            final long previousId = sequence.get();
            final long currentId = sequence.incrementAndGet();
            message.setSequenceId(currentId);
            message.setPreviousId(previousId);
            // 如果此消息关联了 UniqueEvent，给 UniqueEvent 加上相同的sequenceId：
            if (unique != null)
                unique.setSequenceId(message.getSequenceId());
            events.add(EventEntity.builder()
                    .previousId(previousId)
                    .sequenceId(currentId)
                    .data(messageHolder.serialize(message))
                    .build());
            sequencedMessages.add(message);
        }
        if (uniques.size() != 0)
            db.insert(uniques);
        db.insert(events);
        return sequencedMessages;
    }

    public long getMaxSequenceId() {
        final EventEntity last = db.from(EventEntity.class).orderBy("sequenceId").desc().first();
        if (last == null) {
            log.info("Can't find max sequenceId, set max sequenceId: 0.");
            return 0;
        }
        this.lastTimestamp = last.getCreateAt();
        log.debug("Find max sequenceId: {}, last timestamp: {}", last.getSequenceId(), this.lastTimestamp);
        return last.getSequenceId();
    }

}
