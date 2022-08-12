package com.illtamer.service.currencytrading.enging.asset;

import java.math.BigDecimal;

/**
 * 资产实体类
 */
public class Asset {

    /**
     * 可用余额
     * */
    private BigDecimal available;

    /**
     * 冻结余额
     * */
    private BigDecimal frozen;

    public Asset() {
        this(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public Asset(BigDecimal available, BigDecimal frozen) {
        this.available = available;
        this.frozen = frozen;
    }

    public void setAvailable(BigDecimal available) {
        this.available = available;
    }

    public void setFrozen(BigDecimal frozen) {
        this.frozen = frozen;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public BigDecimal getFrozen() {
        return frozen;
    }

}
