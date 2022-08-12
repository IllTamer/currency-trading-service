package com.illtamer.service.currencytrading.enging.asset;

import com.illtamer.service.currencytrading.common.enums.AssetEnum;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AssetService {

    private static final Logger log =  LoggerFactory.getLogger(AssetService.class);

    /**
     * 用户资产结构
     * <p>
     * 用户ID -> (资产ID -> Asset)
     * @apiNote ConcurrentHashMap 同步多线程读(防止部分版本 HashMap 死循环)，该业务不允许多线程写
     * */
    private final Map<Long, Map<AssetEnum, Asset>> userAssets = new ConcurrentHashMap<>();

    /**
     * 转账
     * */
    public void transfer(TransferEnum transferType, Long fromUser, Long toUser, AssetEnum assetType, BigDecimal amount) {
        if (!doTryTransfer(transferType, fromUser, toUser, assetType, amount, true)) {
            log.error("Transfer failed for {}, from user {} to user {}, asset {}, amount {}", transferType, fromUser, toUser, assetType, amount);
        }
        log.debug("Transfer asset {}, from {} => {}, amount {}", assetType, fromUser, toUser, amount);
    }

    /**
     * 尝试转账
     * */
    public boolean tryTransfer(TransferEnum transferType, Long fromUser, Long toUser, AssetEnum assetType, BigDecimal amount, boolean checkBalance) {
        final boolean ok = doTryTransfer(transferType, fromUser, toUser, assetType, amount, checkBalance);
        log.debug("Transfer({}) asset {}, from {} => {}, amount {}", ok, assetType, fromUser, toUser, amount);
        return ok;
    }

    /**
     * 尝试冻结
     * */
    public boolean tryFreeze(Long userId, AssetEnum assetType, BigDecimal amount) {
        final boolean ok = doTryTransfer(TransferEnum.AVAILABLE_TO_FROZEN, userId, userId, assetType, amount, true);
        log.debug("Freeze({}) user {}, asset {}, amount {}", ok, userId, assetType, amount);
        return ok;
    }

    /**
     * 解冻
     * */
    public void unfreeze(Long userId, AssetEnum assetType, BigDecimal amount) {
        if (!doTryTransfer(TransferEnum.FROZEN_TO_AVAILABLE, userId, userId, assetType, amount, true)) {
            log.error("Unfrozen failed for user {}, asset {}, amount {}", userId, assetType, amount);
        }
        log.debug("Unfrozen user {}, asset {}, amount {}", userId, assetType, amount);
    }

    /**
     * @param checkBalance 需要检查余额
     * @apiNote 仅支持单线程调用
     * */
    protected boolean doTryTransfer(TransferEnum transferType, Long fromUser, Long toUser,
                                    AssetEnum assetType, BigDecimal amount, boolean checkBalance) {
        Assert.isTrue(amount.signum() >= 0, "转账金额不为负");
        Asset fromAsset = getOrCreatAsset(fromUser, assetType);
        Asset toAsset = getOrCreatAsset(toUser, assetType);
        return switch(transferType) {
            case AVAILABLE_TO_AVAILABLE -> {
                if (checkBalance && fromAsset.getAvailable().compareTo(amount) < 0) {
                    yield false;
                }
                fromAsset.setAvailable(fromAsset.getAvailable().subtract(amount));
                toAsset.setAvailable(toAsset.getAvailable().add(amount));
                yield true;
            }
            case AVAILABLE_TO_FROZEN -> {
                if (checkBalance && fromAsset.getAvailable().compareTo(amount) < 0) {
                    yield false;
                }
                fromAsset.setAvailable(fromAsset.getAvailable().subtract(amount));
                toAsset.setFrozen(toAsset.getFrozen().add(amount));
                yield true;
            }
            case FROZEN_TO_AVAILABLE -> {
                if (checkBalance && fromAsset.getFrozen().compareTo(amount) < 0) {
                    yield false;
                }
                fromAsset.setFrozen(fromAsset.getFrozen().subtract(amount));
                toAsset.setAvailable(toAsset.getAvailable().add(amount));
                yield true;
            }
        };
    }

    @NotNull
    public Asset getOrCreatAsset(Long userId, AssetEnum assetType) {
        Asset asset = getAssets(userId).get(assetType);
        if (asset == null)
            asset = initAsset(userId, assetType);
        return asset;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public Map<AssetEnum, Asset> getAssets(Long userId) {
        final Map<AssetEnum, Asset> assets = userAssets.get(userId);
        return assets == null ? Collections.EMPTY_MAP : assets;
    }

    public Map<Long, Map<AssetEnum, Asset>> getUserAssets() {
        return userAssets;
    }

    private Asset initAsset(Long userId, AssetEnum assetType) {
        Asset zeroAsset = new Asset();
        userAssets.computeIfAbsent(userId, key -> new ConcurrentHashMap<>())
                .put(assetType, zeroAsset);
        return zeroAsset;
    }

}
