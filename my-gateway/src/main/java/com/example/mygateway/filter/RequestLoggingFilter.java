package com.example.mygateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 请求日志过滤器
 * 记录所有请求的开始、成功和失败信息
 */
@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();
        String requestId = System.currentTimeMillis() + "-" + request.getId();

        log.info("Gateway请求开始 - RequestId: {}, Method: {}, Path: {}, Query: {}",
                requestId, request.getMethod(), request.getPath(), request.getQueryParams());

        return chain.filter(exchange)
                .doOnSuccess(aVoid -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Gateway请求成功 - RequestId: {}, Method: {}, Path: {}, Duration: {}ms",
                            requestId, request.getMethod(), request.getPath(), duration);
                })
                .doOnError(throwable -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Gateway请求失败 - RequestId: {}, Method: {}, Path: {}, Duration: {}ms, Error: {}",
                            requestId, request.getMethod(), request.getPath(), duration, throwable.getMessage());
                });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
