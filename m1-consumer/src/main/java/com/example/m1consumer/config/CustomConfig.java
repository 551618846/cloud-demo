package com.example.m1consumer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 自定义配置类
 * 支持配置自动刷新
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "custom.config")
@Data
public class CustomConfig {

    /**
     * 应用名称
     */
    private String appName = "m1-consumer-service";

    /**
     * 环境
     */
    private String env = "dev";

    /**
     * 描述信息
     */
    private String description = "默认配置";

    /**
     * 功能开关
     */
    private boolean featureEnabled = true;

    /**
     * 超时时间（秒）
     */
    private int timeout = 30;

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;
}
