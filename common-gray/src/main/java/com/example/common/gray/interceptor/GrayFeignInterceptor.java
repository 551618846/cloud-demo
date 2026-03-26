package com.example.common.gray.interceptor;

import com.example.common.gray.constant.GrayConstant;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign灰度标记拦截器
 * 将当前请求的灰度标记传递到Feign请求中
 */
@Slf4j
public class GrayFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // 获取当前请求上下文
        ServletRequestAttributes attributes = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        HttpServletRequest request = attributes.getRequest();

        // 传递灰度相关头部
        copyHeader(request, template, GrayConstant.GRAY_VERSION_HEADER);
        copyHeader(request, template, GrayConstant.GRAY_USER_ID_HEADER);
        copyHeader(request, template, GrayConstant.GRAY_TAG_HEADER);
        copyHeader(request, template, GrayConstant.GRAY_RULE_HEADER);

        if (log.isDebugEnabled()) {
            log.debug("Feign请求传递灰度标记: url={}, headers={}",
                    template.url(), template.headers());
        }
    }

    private void copyHeader(HttpServletRequest request, RequestTemplate template, String headerName) {
        String headerValue = request.getHeader(headerName);
        if (headerValue != null && !headerValue.trim().isEmpty()) {
            template.header(headerName, headerValue);
            log.trace("传递灰度头部: {} = {}", headerName, headerValue);
        }
    }
}
