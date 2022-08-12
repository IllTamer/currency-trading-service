package com.illtamer.service.currencytrading.common.message.event;

import com.illtamer.service.currencytrading.common.message.AbstractMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.Nullable;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractEvent extends AbstractMessage {

    /**
     * 定序后的 Sequence ID
     * */
    private long sequenceId;

    /**
     * 定序后的 Previous Sequence ID
     * */
    private long previousId;

    /**
     * 可选的全局唯一标识
     * */
    @Nullable
    private String uniqueId;

}
