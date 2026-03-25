package com.example.mygateway.service;

import com.example.mygateway.config.GrayConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

/**
 * 灰度服务管理器
 * 管理灰度规则和灰度配置
 */
@Slf4j
@Service
@RefreshScope
public class GrayServiceManager {
    
    @Autowired
    private GrayConfig grayConfig;
    
    /**
     * 检查灰度是否启用
     */
    public boolean isGrayEnabled() {
        return Boolean.TRUE.equals(grayConfig.getEnabled());
    }
    
    /**
     * 获取灰度配置
     */
    public GrayConfig getGrayConfig() {
        return grayConfig;
    }
    
    /**
     * 刷新灰度规则
     */
    public void refreshGrayRules() {
        log.info("刷新灰度规则: enabled={}, rules count={}", 
                grayConfig.getEnabled(),
                grayConfig.getRules() != null ? grayConfig.getRules().size() : 0);
    }
    
    /**
     * 添加灰度规则
     */
    public void addGrayRule(GrayConfig.GrayRule rule) {
        grayConfig.getRules().add(rule);
        log.info("添加灰度规则: name={}, type={}, service={}", 
                rule.getName(), rule.getRuleType(), rule.getServiceName());
    }
    
    /**
     * 移除灰度规则
     */
    public void removeGrayRule(String ruleName) {
        if (grayConfig.getRules() != null) {
            grayConfig.getRules().removeIf(rule -> rule.getName().equals(ruleName));
            log.info("移除灰度规则: name={}", ruleName);
        }
    }
    
    /**
     * 启用/禁用灰度
     */
    public void setGrayEnabled(boolean enabled) {
        grayConfig.setEnabled(enabled);
        log.info("灰度功能已{}", enabled ? "启用" : "禁用");
    }
}
