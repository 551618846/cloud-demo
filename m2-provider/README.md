# M2-Provider Module - Service Provider

## 简介

m2-provider 模块是一个服务提供者，实现了 m1-consumer 模块调用的远程服务接口。

## 技术栈

- Spring Boot 4.0.4
- Spring Cloud 2025.1.0
- Spring Cloud Alibaba 2025.0.0.0
- Nacos 配置中心和服务发现

## 主要功能

- 提供远程服务接口
- 注册到 Nacos 服务发现中心
- 配置管理通过 Nacos 配置中心

## 项目结构

```
m2-provider/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/m2provider/
│   │   │       ├── M2ProviderApplication.java      # 主启动类
│   │   │       └── controller/
│   │   │           └── DemoController.java          # 控制器
│   │   └── resources/
│   │       └── application.properties               # 应用配置（含Nacos配置）
└── pom.xml
```

## 提供的接口

### 1. 获取Demo信息
```
GET /api/demo/{id}
```
响应示例：
```
Hello from m2-provider! You requested id: 123
```

### 2. Hello接口
```
GET /api/demo/hello
```
响应示例：
```
Hello from m2-provider service!
```

## 快速开始

### 1. 启动服务

```bash
cd m2-provider
mvn spring-boot:run
```

服务将在 8082 端口启动。

### 2. 测试接口

```bash
# 测试获取Demo信息接口
curl http://localhost:8082/api/demo/123

# 测试Hello接口
curl http://localhost:8082/api/demo/hello
```

## 配置说明

在 `application.properties` 中配置了：

```properties
server.port=8082

spring.application.name=m2-provider-service

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
```

## 服务调用关系

```
m1-consumer (消费者)
    ↓ (通过Feign客户端调用)
    服务名: m2-provider-service
    ↓
m2-provider (提供者)
```

## 注意事项

1. 确保在主启动类上添加 `@EnableDiscoveryClient` 注解
2. 确保 Nacos 服务器已在 `127.0.0.1:8848` 运行
3. 服务名称必须为 `m2-provider-service`，这样 m1-consumer 才能通过服务名调用
4. 端口号为 8082，避免与 m1-consumer 的 8081 冲突
