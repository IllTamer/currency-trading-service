package com.illtamer.service.currencytrading.common.model.trade;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Builder
@Data
@NoArgsConstructor
@Entity
@Table(name = "unique_events")
public class UniqueEventEntity {

    @Id
//    @Column(nullable = false, updatable = false, length = )
    private String uniqueId;

    /**
     * Which event associated
     * */
    @Column(nullable = false, updatable = false)
    private Long sequenceId;

    /**
     * Created time (milliseconds). Set after sequenced.
     * */
    @Column(nullable = false, updatable = false)
    private Long createdAt;

}
