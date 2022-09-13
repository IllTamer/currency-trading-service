package com.illtamer.service.currencytrading.common.exception;

public record APIErrorResponse(
        APIError error,
        String data,
        String message
) {
}
