package com.example.common.logging.util;

import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;

/**
 * MDC工具类
 * 用于在日志中统一添加跟踪ID
 */
public class MDCUtil {

    public static final String REQUEST_ID_KEY = "requestId";
    public static final String USER_ID_KEY = "userId";
    public static final String TRACE_ID_KEY = "traceId";
    public static final String SPAN_ID_KEY = "spanId";

    /**
     * 设置请求ID
     */
    public static void setRequestId(String requestId) {
        if (requestId != null && !requestId.trim().isEmpty()) {
            MDC.put(REQUEST_ID_KEY, requestId);
        }
    }

    /**
     * 生成并设置请求ID
     */
    public static String generateAndSetRequestId() {
        String requestId = UUID.randomUUID().toString();
        setRequestId(requestId);
        return requestId;
    }

    /**
     * 获取当前请求ID
     */
    public static String getRequestId() {
        return MDC.get(REQUEST_ID_KEY);
    }

    /**
     * 设置用户ID
     */
    public static void setUserId(String userId) {
        if (userId != null && !userId.trim().isEmpty()) {
            MDC.put(USER_ID_KEY, userId);
        }
    }

    /**
     * 获取用户ID
     */
    public static String getUserId() {
        return MDC.get(USER_ID_KEY);
    }

    /**
     * 设置跟踪ID（用于分布式链路追踪）
     */
    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.trim().isEmpty()) {
            MDC.put(TRACE_ID_KEY, traceId);
        }
    }

    /**
     * 获取跟踪ID
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    /**
     * 设置Span ID（用于分布式链路追踪）
     */
    public static void setSpanId(String spanId) {
        if (spanId != null && !spanId.trim().isEmpty()) {
            MDC.put(SPAN_ID_KEY, spanId);
        }
    }

    /**
     * 获取Span ID
     */
    public static String getSpanId() {
        return MDC.get(SPAN_ID_KEY);
    }

    /**
     * 清除所有MDC值
     */
    public static void clear() {
        MDC.clear();
    }

    /**
     * 获取当前MDC上下文的快照
     */
    public static Map<String, String> getCopyOfContextMap() {
        return MDC.getCopyOfContextMap();
    }

    /**
     * 设置MDC上下文（用于线程间传递）
     */
    public static void setContextMap(Map<String, String> contextMap) {
        if (contextMap != null && !contextMap.isEmpty()) {
            MDC.setContextMap(contextMap);
        }
    }
}