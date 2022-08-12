package com.illtamer.service.currencytrading.common.model.trade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.illtamer.service.currencytrading.common.enums.DirectionEnum;
import com.illtamer.service.currencytrading.common.enums.OrderStatus;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "orders")
public class OrderEntity implements Comparable<OrderEntity> {

    /**
     * 订单ID
     * */
    @Id
    @Column(nullable = false, updatable = false)
    private Long id;

    /**
     * 定序ID
     * <p>
     * 全局唯一
     * */
    @Column(nullable = false, updatable = false)
    private Long sequenceId;

    /**
     * 用户ID
     * */
    private Long userId;

    /**
     * 价格
     * */
    private BigDecimal price;

    /**
     * 方向
     * */
//    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    private DirectionEnum direction;

    /**
     * 状态
     * */
    private OrderStatus status;

    /**
     * 订单数量
     * */
    private BigDecimal quantity;

    /**
     * 未成交数量
     * */
    private BigDecimal unfilledQuantity;

    /**
     * 创建时间
     * <p>
     * 时间本身实际上是订单的一个普通属性，仅展示给用户，不参与业务排序
     * */
    private long createdAt;

    /**
     * 更新时间
     * */
    private long updatedAt;

    @Transient
    @JsonIgnore
    private int version;

    public void updateOrder(BigDecimal unfilledQuantity, OrderStatus status, long updatedAt) {
        this.unfilledQuantity = unfilledQuantity;
        this.status = status;
        this.updatedAt = updatedAt;
        ++ version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof OrderEntity e) {
            return this.id.longValue() == e.id.longValue();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * 按 OrderID 排序
     * */
    @Override
    public int compareTo(@NotNull OrderEntity o) {
        return Long.compare(id, o.id);
    }

}
