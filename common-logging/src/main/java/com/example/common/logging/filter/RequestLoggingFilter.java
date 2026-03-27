package com.example.common.logging.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Enumeration;
import java.util.UUID;

/**
 * 通用请求日志过滤器
 * 记录所有HTTP请求的详细信息，包括请求头、响应状态、耗时等
 * 支持MDC跟踪ID，便于链路追踪
 */
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_REQUEST_ID_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String requestId = getOrGenerateRequestId(request);
//        MDC.put(MDC_REQUEST_ID_KEY, requestId);

        // 包装请求和响应以支持内容缓存（用于记录请求/响应体）
        boolean isFirstRequest = !isAsyncDispatch(request);
        HttpServletRequest requestToUse = request;
        HttpServletResponse responseToUse = response;

        if (isFirstRequest && !(request instanceof ContentCachingRequestWrapper)) {
            requestToUse = new ContentCachingRequestWrapper(request,0);
        }
        if (isFirstRequest && !(response instanceof ContentCachingResponseWrapper)) {
            responseToUse = new ContentCachingResponseWrapper(response);
        }

        try {
            // 记录请求开始
            logRequestStart(requestToUse, requestId);
            // 继续过滤器链
            filterChain.doFilter(requestToUse, responseToUse);
        } finally {
            // 记录请求结束
            long duration = System.currentTimeMillis() - startTime;
            logRequestEnd(requestToUse, responseToUse, requestId, duration);
            // 复制响应体到原始响应（如果使用了包装器）
            if (responseToUse instanceof ContentCachingResponseWrapper) {
                ContentCachingResponseWrapper responseWrapper = (ContentCachingResponseWrapper) responseToUse;
                responseWrapper.copyBodyToResponse();
            }
//            MDC.clear();
        }
    }

    private String getOrGenerateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        return requestId;
    }

    private void logRequestStart(HttpServletRequest request, String requestId) {
        if (log.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("请求开始Request started - ");
            sb.append("RequestId: ").append(requestId).append(", ");
            sb.append("Method: ").append(request.getMethod()).append(", ");
            sb.append("URI: ").append(request.getRequestURI());
            String queryString = request.getQueryString();
            if (queryString != null) {
                sb.append('?').append(queryString);
            }
            sb.append(", ");
            sb.append("ClientIP: ").append(getClientIp(request));
            sb.append(", ");
            sb.append("Headers: ").append(getHeadersAsString(request));
            log.info(sb.toString());
        }
    }

    private void logRequestEnd(HttpServletRequest request,
                               HttpServletResponse response,
                               String requestId,
                               long duration) {
        if (log.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Request completed - ");
            sb.append("RequestId: ").append(requestId).append(", ");
            sb.append("Method: ").append(request.getMethod()).append(", ");
            sb.append("URI: ").append(request.getRequestURI()).append(", ");
            sb.append("Status: ").append(response.getStatus()).append(", ");
            sb.append("Duration: ").append(duration).append("ms");
            log.info(sb.toString());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String getHeadersAsString(HttpServletRequest request) {
        StringBuilder headers = new StringBuilder("{");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.append(headerName).append(": ").append(request.getHeader(headerName));
            if (headerNames.hasMoreElements()) {
                headers.append(", ");
            }
        }
        headers.append("}");
        return headers.toString();
    }
}