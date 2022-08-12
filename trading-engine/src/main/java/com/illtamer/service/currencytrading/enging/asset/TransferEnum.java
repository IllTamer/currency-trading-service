package com.illtamer.service.currencytrading.enging.asset;

/**
 * 转账类型(资产操作)枚举
 * */
public enum TransferEnum {

    /**
     * 可用转可用
     * */
    AVAILABLE_TO_AVAILABLE,

    /**
     * 可用转冻结
     * */
    AVAILABLE_TO_FROZEN,

    /**
     * 冻结转可用
     * */
    FROZEN_TO_AVAILABLE;

}
