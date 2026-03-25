# M1-Consumer Module - Spring Cloud OpenFeign

## 简介

m1-consumer模块集成了Spring Cloud OpenFeign，提供了声明式HTTP客户端的功能。

## 技术栈

- Spring Boot 4.0.4
- Spring Cloud 2025.1.0
- Spring Cloud OpenFeign

## 主要功能

- Feign客户端定义
- 服务间调用示例
- RESTful API接口

## 项目结构

```
m1-consumer/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/m1consumer/
│   │   │       ├── M1ConsumerApplication.java      # 主启动类
│   │   │       ├── client/
│   │   │       │   └── DemoFeignClient.java    # Feign客户端接口
│   │   │       └── controller/
│   │   │           └── DemoController.java      # 控制器
│   │   └── resources/
│   │       └── application.properties       # 配置文件
│   └── test/
│       └── java/
│           └── com/example/m1consumer/
└── pom.xml
```

## 快速开始

### 1. 启动服务

```bash
cd m1-consumer
mvn spring-boot:run
```

服务将在 8081 端口启动。

### 2. 测试接口

```bash
# 测试本地接口
curl http://localhost:8081/api/m1-consumer/local

# 测试Feign客户端调用
curl http://localhost:8081/api/m1-consumer/hello
```

## Feign客户端使用说明

### 创建Feign客户端

```java
@FeignClient(name = "service-name")
public interface YourClient {
    @GetMapping("/api/endpoint")
    ResponseEntity<String> getData();
}
```

### 启用Feign客户端

在主启动类上添加 `@EnableFeignClients` 注解：

```java
@SpringBootApplication
@EnableFeignClients
public class M1ConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(M1ConsumerApplication.class, args);
    }
}
```

### 使用Feign客户端

```java
@Autowired
private YourClient yourClient;

public String callService() {
    return yourClient.getData();
}
```

## 配置说明

在 `application.properties` 中可以配置：

```properties
server.port=8081

spring.application.name=m1-consumer-service

spring.cloud.nacos.config.server-addr=127.0.0.1:8848
spring.cloud.nacos.config.file-extension=yaml
spring.cloud.nacos.config.namespace=public
spring.cloud.nacos.config.group=DEFAULT_GROUP
spring.cloud.nacos.config.refresh-enabled=true
spring.cloud.nacos.config.import-check.enabled=false

spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848
spring.cloud.nacos.discovery.namespace=public
spring.cloud.nacos.discovery.group=DEFAULT_GROUP
spring.cloud.nacos.discovery.enabled=true

# Feign配置（可选）
feign.client.config.default.connectTimeout=5000
feign.client.config.default.readTimeout=5000
```

## 注意事项

1. 确保在主启动类上添加 `@EnableFeignClients` 注解
2. Feign客户端接口需要使用 `@FeignClient` 注解
3. 需要配合服务发现组件（如Eureka、Nacos）使用，或配置目标服务URL
4. 如需配置超时时间、日志级别等，可在配置文件中添加相应配置
