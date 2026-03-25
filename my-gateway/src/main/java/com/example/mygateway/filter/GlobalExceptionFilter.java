package com.example.mygateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 全局异常过滤器
 * 捕获Gateway路由过程中的异常并返回统一格式
 */
@Slf4j
//@Component
public class GlobalExceptionFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .onErrorResume(throwable -> {
                    log.error("Gateway异常捕获: path={}, error={}",
                            exchange.getRequest().getPath(), throwable.getMessage(), throwable);

                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.OK);
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

                    String errorJson = buildErrorResponse(throwable);
                    DataBuffer buffer = response.bufferFactory().wrap(errorJson.getBytes(StandardCharsets.UTF_8));
                    return response.writeWith(Mono.just(buffer));
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String buildErrorResponse(Throwable throwable) {
        int code = 500;
        String message = "服务器内部错误";

        if (throwable instanceof ResponseStatusException) {
            ResponseStatusException ex = (ResponseStatusException) throwable;
            code = getStatusCode(ex);
            message = getReason(ex);
        } else if (throwable.getCause() instanceof ResponseStatusException) {
            ResponseStatusException ex = (ResponseStatusException) throwable.getCause();
            code = getStatusCode(ex);
            message = getReason(ex);
        } else if (throwable.getMessage() != null) {
            message = throwable.getMessage();
        }

        return String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null,\"timestamp\":%d}",
                code, message, System.currentTimeMillis());
    }

    private int getStatusCode(ResponseStatusException ex) {
        try {
            return ex.getStatusCode().value();
        } catch (Exception e) {
            return 500;
        }
    }

    private String getReason(ResponseStatusException ex) {
        try {
            String reason = ex.getReason();
            return reason != null ? reason : "请求失败";
        } catch (Exception e) {
            return "请求失败";
        }
    }
}
