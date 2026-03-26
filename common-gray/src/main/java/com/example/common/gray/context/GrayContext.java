package com.example.common.gray.context;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 灰度上下文
 * 在请求链路中传递灰度信息
 */
@Data
@Accessors(chain = true)
public class GrayContext {

    /**
     * 是否灰度请求
     */
    private Boolean isGray;

    /**
     * 目标服务名
     */
    private String serviceName;

    /**
     * 灰度版本
     */
    private String version;

    /**
     * 匹配的规则名称
     */
    private String ruleName;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 自定义标签
     */
    private String tag;

    public static GrayContext create() {
        return new GrayContext();
    }
}
