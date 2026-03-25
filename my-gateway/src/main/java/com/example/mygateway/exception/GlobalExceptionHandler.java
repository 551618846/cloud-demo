package com.example.mygateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WebFlux全局异常处理器
 */
@Slf4j
@Order(-1)
@Component
public class GlobalExceptionHandler implements WebExceptionHandler {

    private final ServerCodecConfigurer serverCodecConfigurer;

    public GlobalExceptionHandler(ServerCodecConfigurer serverCodecConfigurer) {
        this.serverCodecConfigurer = serverCodecConfigurer;
    }

    @Override
    public Mono<Void> handle(org.springframework.web.server.ServerWebExchange exchange, Throwable ex) {
        log.error("Gateway异常处理: path={}, error={}", exchange.getRequest().getPath(), ex.getMessage(), ex);

        Map<String, Object> errorResponse = buildErrorResponse(ex, exchange);

        String jsonBody = toJson(errorResponse);

        exchange.getResponse().setStatusCode(HttpStatus.valueOf((Integer) errorResponse.get("code")));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(jsonBody.getBytes(StandardCharsets.UTF_8)))
        );
    }

    private Map<String, Object> buildErrorResponse(Throwable ex, org.springframework.web.server.ServerWebExchange exchange) {
        Map<String, Object> errorResponse = new LinkedHashMap<>();

        if (ex instanceof GatewayException) {
            GatewayException gatewayEx = (GatewayException) ex;
            errorResponse.put("code", gatewayEx.getCode());
            errorResponse.put("message", gatewayEx.getMessage());
        } else if (ex instanceof org.springframework.web.server.ResponseStatusException) {
            org.springframework.web.server.ResponseStatusException statusEx = (org.springframework.web.server.ResponseStatusException) ex;
            errorResponse.put("code", getStatusCode(statusEx));
            errorResponse.put("message", getReason(statusEx));
        } else {
            errorResponse.put("code", 500);
            errorResponse.put("message", ex.getMessage() != null ? ex.getMessage() : "服务器内部错误");
        }

        errorResponse.put("data", null);
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", exchange.getRequest().getPath().value());

        return errorResponse;
    }

    private int getStatusCode(org.springframework.web.server.ResponseStatusException ex) {
        try {
            return ex.getStatusCode().value();
        } catch (Exception e) {
            return 500;
        }
    }

    private String getReason(org.springframework.web.server.ResponseStatusException ex) {
        try {
            String reason = ex.getReason();
            return reason != null ? reason : "请求失败";
        } catch (Exception e) {
            return "请求失败";
        }
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value == null) {
                sb.append("null");
            } else {
                sb.append(value);
            }
        }
        sb.append("}");
        return sb.toString();
    }
}

