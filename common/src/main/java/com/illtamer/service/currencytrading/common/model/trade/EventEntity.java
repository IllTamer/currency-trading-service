package com.illtamer.service.currencytrading.common.model.trade;

import lombok.*;

import javax.persistence.*;

/**
 * 事件对应的数据库 Entity
 * */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "events", uniqueConstraints = @UniqueConstraint(name = "UNI_PREV_ID", columnNames = "previousId"))
public class EventEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private Long sequenceId;

    /**
     * Keep previous id. The previous id of first event is 0
     * */
    @Column(nullable = false, updatable = false)
    private Long previousId;

    /**
     * JSON-encoded event data
     * */
//    @Column(nullable = false, updatable = false, length = )
    private String data;

    @Column(nullable = false, updatable = false)
    private Long createAt;

}
