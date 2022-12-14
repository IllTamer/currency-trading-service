package com.illtamer.service.currencytrading.common.context;

import com.illtamer.service.currencytrading.common.exception.APIError;
import com.illtamer.service.currencytrading.common.exception.APIException;

/**
 * Holds user context in thread-local.
 */
public class UserContext implements AutoCloseable {

    private static final ThreadLocal<Long> THREAD_LOCAL_CTX = new ThreadLocal<>();

    public UserContext(Long userId) {
        THREAD_LOCAL_CTX.set(userId);
    }

    /**
     * Get current user id, or throw exception if no user.
     */
    public static Long getRequiredUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new APIException(APIError.AUTH_SIGNIN_REQUIRED, null, "Need sign-in first.");
        }
        return userId;
    }

    /**
     * Get current user id, or null if no user.
     */
    public static Long getUserId() {
        return THREAD_LOCAL_CTX.get();
    }

    @Override
    public void close() {
        THREAD_LOCAL_CTX.remove();
    }

}
