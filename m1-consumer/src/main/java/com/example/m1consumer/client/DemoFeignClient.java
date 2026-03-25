package com.example.m1consumer.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 示例Feign客户端
 * 使用@FeignClient注解声明这是一个Feign客户端
 * value属性指定要调用的服务名称
 */
@FeignClient(name = "m2-provider-service")
public interface DemoFeignClient {

    /**
     * 调用远程服务的接口示例
     * @param id 路径参数
     * @return 返回结果
     */
    @GetMapping("/api/demo/{id}")
    String getDemo(@PathVariable("id") String id);

    /**
     * 简单的GET请求示例
     * @return 返回结果
     */
    @GetMapping("/api/demo/hello")
    String sayHello();

}
