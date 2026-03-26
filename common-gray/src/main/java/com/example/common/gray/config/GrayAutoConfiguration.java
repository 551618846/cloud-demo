package com.example.common.gray.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 灰度模块自动配置
 * 当引入common-gray依赖时，根据条件自动装配灰度组件
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "gray", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GrayAutoConfiguration {

    /**
     * 注册灰度配置属性
     */
    @Bean
    @ConditionalOnMissingBean
    public GrayConfig grayConfig() {
        return new GrayConfig();
    }

    /**
     * 当classpath中存在Feign时，注册灰度Feign拦截器
     */
    @Configuration
    @ConditionalOnClass(name = "feign.RequestInterceptor")
    static class FeignInterceptorConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public com.example.common.gray.interceptor.GrayFeignInterceptor grayFeignInterceptor() {
            return new com.example.common.gray.interceptor.GrayFeignInterceptor();
        }
    }
}
