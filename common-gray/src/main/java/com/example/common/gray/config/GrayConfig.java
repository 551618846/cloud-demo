package com.example.common.gray.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * 灰度配置类
 * 定义灰度规则类型和配置
 */
@Data
@ConfigurationProperties(prefix = "gray")
public class GrayConfig {

    /**
     * 是否启用灰度
     */
    private Boolean enabled = true;

    /**
     * 灰度规则列表
     */
    private List<GrayRule> rules;

    /**
     * 灰度规则
     */
    @Data
    public static class GrayRule {
        /**
         * 规则名称
         */
        private String name;

        /**
         * 服务名（支持通配符，如 m1-consumer-service）
         */
        private String serviceName;

        /**
         * 灰度规则类型
         */
        private GrayRuleType ruleType;

        /**
         * 灰度配置（根据规则类型不同，配置不同）
         */
        private Map<String, String> config;

        /**
         * 优先级（数字越小优先级越高）
         */
        private Integer priority = 100;

        /**
         * 是否启用
         */
        private Boolean enabled = true;
    }

    /**
     * 灰度规则类型
     */
    public enum GrayRuleType {
        /**
         * 用户ID白名单
         */
        USER_ID,

        /**
         * 流量百分比
         */
        PERCENTAGE,

        /**
         * 自定义标签
         */
        TAG,

        /**
         * IP白名单
         */
        IP
    }
}
