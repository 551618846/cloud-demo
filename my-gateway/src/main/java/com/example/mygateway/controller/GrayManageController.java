package com.example.mygateway.controller;

import com.example.common.gray.config.GrayConfig;
import com.example.mygateway.service.GrayServiceManager;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 灰度管理控制器
 * 提供灰度规则管理和查询接口
 */
@RestController
@RequestMapping("/gray/manage")
public class GrayManageController {
    
    private final GrayServiceManager grayServiceManager;
    
    public GrayManageController(GrayServiceManager grayServiceManager) {
        this.grayServiceManager = grayServiceManager;
    }
    
    /**
     * 获取灰度配置
     */
    @GetMapping("/config")
    public Mono<Map<String, Object>> getGrayConfig() {
        Map<String, Object> result = new HashMap<>();
        GrayConfig config = grayServiceManager.getGrayConfig();
        result.put("enabled", config.getEnabled());
        result.put("rules", config.getRules());
        return Mono.just(result);
    }
    
    /**
     * 启用/禁用灰度
     */
    @PostMapping("/toggle")
    public Mono<Map<String, Object>> toggleGray(@RequestParam boolean enabled) {
        grayServiceManager.setGrayEnabled(enabled);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("enabled", enabled);
        result.put("message", enabled ? "灰度功能已启用" : "灰度功能已禁用");
        return Mono.just(result);
    }
    
    /**
     * 添加灰度规则
     */
    @PostMapping("/rule")
    public Mono<Map<String, Object>> addRule(@RequestBody GrayRuleRequest request) {
        GrayConfig.GrayRule rule = new GrayConfig.GrayRule();
        rule.setName(request.getName());
        rule.setServiceName(request.getServiceName());
        rule.setRuleType(request.getRuleType());
        rule.setConfig(request.getConfig());
        rule.setPriority(request.getPriority());
        rule.setEnabled(request.getEnabled());
        
        grayServiceManager.addGrayRule(rule);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "灰度规则添加成功");
        return Mono.just(result);
    }
    
    /**
     * 删除灰度规则
     */
    @DeleteMapping("/rule/{ruleName}")
    public Mono<Map<String, Object>> deleteRule(@PathVariable String ruleName) {
        grayServiceManager.removeGrayRule(ruleName);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "灰度规则删除成功");
        return Mono.just(result);
    }
    
    /**
     * 刷新灰度规则
     */
    @PostMapping("/refresh")
    public Mono<Map<String, Object>> refreshRules() {
        grayServiceManager.refreshGrayRules();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "灰度规则刷新成功");
        return Mono.just(result);
    }
    
    /**
     * 灰度规则请求
     */
    @Data
    @AllArgsConstructor
    public static class GrayRuleRequest {
        private String name;
        private String serviceName;
        private GrayConfig.GrayRuleType ruleType;
        private Map<String, String> config;
        private Integer priority = 100;
        private Boolean enabled = true;
    }
}
