package com.illtamer.service.currencytrading.api;

import com.illtamer.service.currencytrading.common.exception.APIError;
import com.illtamer.service.currencytrading.common.exception.APIException;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class TradingEngineApiProxyService {

    private static final Logger log = LoggerFactory.getLogger(TradingEngineApiProxyService.class);

    @Value("#{exchangeConfiguration.apiEndpoints.tradingEngineApi}")
    private String tradingEngineInternalApiEndpoint;

    private final OkHttpClient okhttpClient = new OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(20, 60, TimeUnit.SECONDS))
            .retryOnConnectionFailure(false)
            .build();

    public String get(String url) throws IOException {
        Request request = new Request.Builder().url(tradingEngineInternalApiEndpoint + url).header("Accept", "*/*")
                .build();
        try (Response response = okhttpClient.newCall(request).execute()) {
            if (response.code() != 200) {
                log.error("Internal api failed with code {}: {}", response.code(), url);
                throw new APIException(APIError.OPERATION_TIMEOUT, null, "operation timeout.");
            }
            try (ResponseBody body = response.body()) {
                if (body == null || body.string().isEmpty()) {
                    log.error("Internal api failed with code 200 but empty response");
                    throw new APIException(APIError.INTERNAL_SERVER_ERROR, null, "response is empty.");
                }
                return body.string();
            }
        }
    }

}
