package com.illtamer.service.currencytrading.common.exception;

public class APIException extends RuntimeException {

    private final APIErrorResponse error;

    public APIException(APIError error) {
        super(error.toString());
        this.error = new APIErrorResponse(error, null, "");
    }

    public APIException(APIError error, String data) {
        super(error.toString());
        this.error = new APIErrorResponse(error, data, "");
    }

    public APIException(APIError error, String data, String message) {
        super(message);
        this.error = new APIErrorResponse(error, data, message);
    }

}