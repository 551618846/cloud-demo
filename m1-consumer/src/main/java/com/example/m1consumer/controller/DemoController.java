package com.example.m1consumer.controller;

import com.alibaba.cloud.nacos.annotation.NacosConfig;
import com.example.m1consumer.client.DemoFeignClient;
import com.example.m1consumer.config.CustomConfig;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 示例控制器
 * 展示如何注入和使用Feign客户端
 */
@RestController
@RequestMapping("/api/m1-consumer")
public class DemoController {

    @Value("${demo.conf:}")//不能刷新
    private String conf;
    @Value("${server.port}")//不能刷新
    private String serverPort;

    @NacosConfig(group = "DEFAULT_GROUP", dataId = "my-common.properties",key = "demo.conf2")//能刷新
    private String conf2;

    @Autowired
    private CustomConfig customConfig;//自动刷新

    @Autowired
    private DemoFeignClient demoFeignClient;

    /**
     * 通过Feign客户端调用远程服务
     * @param id 参数
     * @return 远程服务返回的结果
     */
    @GetMapping("/demo/{id}")
    public String callDemoService(@PathVariable String id) {
        return demoFeignClient.getDemo(id);
    }

    /**
     * 通过Feign客户端调用远程服务
     * @return 远程服务返回的结果
     */
    @GetMapping("/hello")
    public String callHello() {
        return demoFeignClient.sayHello();
    }

    /**
     * 本地接口示例
     * @return 消息
     */
    @GetMapping("/local")
    public String localEndpoint() {
        Map<String, String> context = MDC.getCopyOfContextMap();
        System.out.println("MDC: " + context);
        return "This is a local endpoint from m1-consumer service:" + conf + ",:conf2=" + conf2+",serverPort="+serverPort;
    }

    /**
     * 配置接口示例
     * @return 配置信息
     */
    @GetMapping("/config")
    public String getConfig() {
        return customConfig.toString();
    }

    /**
     * 测试配置刷新接口
     * @return 配置信息
     */
    @GetMapping("/config/test")
    public String testConfigRefresh() {
        return String.format("配置信息 - AppName: %s, Env: %s, Description: %s, FeatureEnabled: %s, Timeout: %ds, MaxRetries: %d",
                customConfig.getAppName(),
                customConfig.getEnv(),
                customConfig.getDescription(),
                customConfig.isFeatureEnabled(),
                customConfig.getTimeout(),
                customConfig.getMaxRetries());
    }
}
