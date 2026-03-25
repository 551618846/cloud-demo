# 全链路灰度发布使用指南

## 概述

本项目实现了完整的全链路灰度发布功能，支持多种灰度策略，包括：
- 用户ID白名单灰度
- 流量百分比灰度
- 自定义标签灰度
- IP白名单灰度

## 核心组件

### 1. 灰度配置类 (GrayConfig)
定义灰度规则类型和配置结构。

### 2. 灰度过滤器 (GrayReleaseFilter)
在网关层对请求进行灰度标记，根据规则判断请求是否应该路由到灰度版本。

### 3. 灰度负载均衡器 (GrayLoadBalancer)
根据灰度标记选择对应版本的服务实例。

### 4. 灰度服务管理器 (GrayServiceManager)
管理灰度规则和灰度配置，支持动态刷新。

### 5. 灰度管理控制器 (GrayManageController)
提供灰度规则管理和查询接口。

## 使用步骤

### 1. 配置灰度规则

#### 方式一：在Nacos配置中心配置

1. 登录Nacos控制台
2. 进入配置管理 → 配置列表
3. 点击创建配置
4. Data ID: `my-gateway-service-dev.properties`
5. Group: `DEFAULT_GROUP`
6. 配置格式: `Properties`
7. 配置内容参考 `nacos-gray-config-example.properties`

#### 方式二：在application.properties中配置

参考 `application.properties` 中的示例配置。

### 2. 启动带版本的服务实例

在服务启动时，需要通过Nacos元数据指定服务版本：

```java
@SpringBootApplication
public class M1ConsumerApplication {
    public static void main(String[] args) {
        // 设置服务版本元数据
        System.setProperty("spring.cloud.nacos.discovery.metadata.version", "gray-v1");
        SpringApplication.run(M1ConsumerApplication.class, args);
    }
}
```

或者在配置文件中配置：

```properties
spring.cloud.nacos.discovery.metadata.version=gray-v1
```

### 3. 发起灰度请求

#### 用户ID白名单灰度

```
# 请求头方式
curl -H "X-User-Id: user001" http://localhost/api/m1-consumer/hello

# 请求参数方式
curl http://localhost/api/m1-consumer/hello?userId=user001
```

#### 流量百分比灰度

普通请求即可，系统会根据用户ID计算hash值，将指定百分比的流量路由到灰度版本：

```
curl http://localhost/api/m1-consumer/hello?userId=user123
```

#### 自定义标签灰度

```
# 请求头方式
curl -H "X-Gray-Tag: beta" http://localhost/api/m1-consumer/hello

# 请求参数方式
curl http://localhost/api/m1-consumer/hello?grayTag=beta
```

#### IP白名单灰度

从指定IP发起的请求会自动路由到灰度版本：

```
curl http://localhost/api/m1-consumer/hello
```

## 管理接口

### 查看灰度配置

```
GET http://localhost/gray/manage/config
```

响应示例：

```json
{
  "enabled": true,
  "rules": [
    {
      "name": "user-white-list",
      "serviceName": "m1-consumer-service",
      "ruleType": "USER_ID",
      "config": {
        "whiteList": "user001,user002,user003",
        "version": "gray-v1"
      },
      "priority": 10,
      "enabled": true
    }
  ]
}
```

### 启用/禁用灰度

```
POST http://localhost/gray/manage/toggle?enabled=true
```

### 添加灰度规则

```
POST http://localhost/gray/manage/rule
Content-Type: application/json

{
  "name": "new-rule",
  "serviceName": "m1-consumer-service",
  "ruleType": "PERCENTAGE",
  "config": {
    "percentage": "20",
    "version": "gray-v2"
  },
  "priority": 20,
  "enabled": true
}
```

### 删除灰度规则

```
DELETE http://localhost/gray/manage/rule/{ruleName}
```

### 刷新灰度规则

```
POST http://localhost/gray/manage/refresh
```

## 灰度规则优先级

灰度规则按 `priority` 值从小到大匹配，数字越小优先级越高。默认优先级为100。

建议优先级设置：
- IP白名单: 5
- 用户ID白名单: 10
- 百分比灰度: 20
- 标签灰度: 30

## 日志说明

### 网关日志

灰度请求会记录以下日志：

```
灰度请求: service=m1-consumer-service, version=gray-v1, userId=user001, ip=192.168.1.100, rule=user-white-list
```

### 负载均衡日志

灰度负载均衡会记录以下日志：

```
灰度负载均衡: service=m1-consumer-service, version=gray-v1, instance=192.168.1.101:8081
```

## 最佳实践

1. **渐进式灰度**：
   - 先从IP白名单开始，内部测试
   - 然后使用用户ID白名单，让特定用户体验
   - 最后使用百分比灰度，逐步扩大范围

2. **版本命名规范**：
   - 稳定版本: `stable` 或 `v1.0`
   - 灰度版本: `gray-v1`, `gray-v2`
   - 金丝雀版本: `canary-v1`

3. **监控和回滚**：
   - 密切监控灰度版本的性能和错误率
   - 发现问题及时调整灰度百分比或禁用灰度
   - 准备快速回滚方案

4. **配置管理**：
   - 使用Nacos配置中心管理灰度规则
   - 支持动态刷新，无需重启服务
   - 建议不同环境使用不同的灰度配置

## 注意事项

1. 服务实例必须配置版本元数据：`spring.cloud.nacos.discovery.metadata.version`
2. 灰度规则中的 `version` 必须与服务实例的元数据版本一致
3. 如果没有匹配的灰度实例，会降级到普通实例
4. 灰度配置支持Nacos动态刷新，修改后自动生效
5. 建议在灰度期间增加日志和监控，及时发现问题

## 故障排查

### 问题1：请求没有路由到灰度版本

检查项：
1. 灰度是否启用：`gray.enabled=true`
2. 灰度规则是否配置正确
3. 服务实例是否配置了版本元数据
4. 灰度规则中的版本名是否与实例元数据一致

### 问题2：灰度规则不生效

检查项：
1. 灰度规则的 `enabled` 是否为 `true`
2. 灰度规则的 `serviceName` 是否匹配请求的目标服务
3. 用户ID/IP/标签是否在白名单中
4. 百分比配置是否正确（0-100）

### 问题3：找不到灰度实例

检查项：
1. 灰度版本的服务实例是否已启动并注册到Nacos
2. 服务实例的版本元数据是否配置正确
3. 查看负载均衡日志，确认是否找到灰度实例
