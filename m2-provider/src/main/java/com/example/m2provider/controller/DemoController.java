package com.example.m2provider.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 示例控制器
 * 提供远程服务接口
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    /**
     * 获取Demo信息
     * @param id 参数
     * @return 返回结果
     */
    @GetMapping("/{id}")
    public String getDemo(@PathVariable String id) {
        return "Hello from m2-provider! You requested id: " + id;
    }

    /**
     * 简单的Hello接口
     * @return 返回结果
     */
    @GetMapping("/hello")
    public String sayHello() {
        return "Hello from m2-provider service!";
    }

}
