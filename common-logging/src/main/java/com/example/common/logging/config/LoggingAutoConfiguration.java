package com.example.common.logging.config;

//import com.example.common.logging.aspect.LoggingAspect;
import com.example.common.logging.aspect.LoggingAspect;
import com.example.common.logging.filter.RequestLoggingFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 日志模块自动配置
 * 当引入common-logging依赖时自动生效
 */
@Configuration
@ConditionalOnWebApplication
@Import({LoggingAspect.class})//建议：实践中可以移除 @Import，直接在 @Bean 方法上声明即可；或者保留 @Import 并删除 @Bean 方法。两种方式均可，但不宜混用
public class LoggingAutoConfiguration {

    /**
     * 注册请求日志过滤器
     */
    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter() {
        FilterRegistrationBean<RequestLoggingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestLoggingFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Integer.MIN_VALUE); // 最高优先级
        registrationBean.setName("requestLoggingFilter");
        return registrationBean;
    }

    /**
     * 提供LoggingAspect bean（已通过@Import导入）
     */
    @Bean
    @ConditionalOnMissingBean
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }
}