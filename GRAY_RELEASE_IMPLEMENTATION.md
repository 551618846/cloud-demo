# 全链路灰度实现方案

## 一、实现概述

本项目实现了一个完整的全链路灰度发布系统，支持多种灰度策略，适用于微服务架构下的渐进式发布。

### 技术栈
- Spring Cloud Gateway 5.0.1
- Spring Boot 4.0.4
- Nacos (服务发现 + 配置中心)
- Spring Cloud LoadBalancer

### 核心特性
1. 多种灰度策略：用户ID白名单、流量百分比、自定义标签、IP白名单
2. 规则优先级管理
3. Nacos动态配置刷新
4. 完整的管理接口
5. 详细的日志记录

## 二、架构设计

### 组件关系

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ HTTP Request
       ↓
┌──────────────────────┐
│  Spring Cloud Gateway│
│                      │
│  ┌────────────────┐  │
│  │ GrayRelease    │  │  ┌─────────────┐
│  │ Filter         │  │  │ GrayConfig  │
│  └────────┬───────┘  │  │             │
│           │          │  └─────────────┘
│           ↓          │
│  ┌────────────────┐  │
│  │ Add Gray       │  │
│  │ Headers        │  │
│  └────────┬───────┘  │
│           │          │
│           ↓          │
│  ┌────────────────┐  │
│  │ GrayLoadBalancer│ │  ┌─────────────────┐
│  └────────┬───────┘  │  │ ServiceInstance │
│           │          │  │ Metadata.version │
│           ↓          │  └─────────────────┘
│  ┌────────────────┐  │
│  │ Select Gray    │  │
│  │ Instance       │  │
│  └────────┬───────┘  │
└───────────┼──────────┘
            │
            ↓
    ┌───────┴───────────┐
    │                   │
┌───▼────┐        ┌────▼───┐
│ Stable │        │  Gray  │
│ Service│        │ Service│
└────────┘        └────────┘
```

## 三、核心组件说明

### 1. GrayConfig (灰度配置)
- 位置: `my-gateway/src/main/java/com/example/mygateway/config/GrayConfig.java`
- 功能: 定义灰度规则的数据结构
- 支持的规则类型:
  - `USER_ID`: 用户ID白名单
  - `PERCENTAGE`: 流量百分比
  - `TAG`: 自定义标签
  - `IP`: IP白名单

### 2. GrayReleaseFilter (灰度过滤器)
- 位置: `my-gateway/src/main/java/com/example/mygateway/filter/GrayReleaseFilter.java`
- 功能:
  - 拦截所有经过网关的请求
  - 根据灰度规则判断请求是否需要灰度
  - 添加灰度标记到请求头
  - 记录灰度请求日志

### 3. GrayLoadBalancer (灰度负载均衡器)
- 位置: `my-gateway/src/main/java/com/example/mygateway/loadbalancer/GrayLoadBalancer.java`
- 功能:
  - 根据灰度标记选择对应版本的服务实例
  - 支持降级到普通实例
  - 提供负载均衡日志

### 4. GrayServiceManager (灰度服务管理器)
- 位置: `my-gateway/src/main/java/com/example/mygateway/service/GrayServiceManager.java`
- 功能:
  - 管理灰度规则
  - 支持动态刷新
  - 提供规则增删改查操作

### 5. GrayManageController (灰度管理控制器)
- 位置: `my-gateway/src/main/java/com/example/mygateway/controller/GrayManageController.java`
- 功能:
  - 提供RESTful管理接口
  - 支持在线查看和修改灰度规则

## 四、配置说明

### 网关配置文件 (application.properties)

```properties
# 灰度开关
gray.enabled=true

# 灰度规则配置
gray.rules[0].name=user-white-list
gray.rules[0].serviceName=m1-consumer-service
gray.rules[0].ruleType=USER_ID
gray.rules[0].config.whiteList=user001,user002
gray.rules[0].config.version=gray-v1
gray.rules[0].priority=10
gray.rules[0].enabled=true
```

### 服务实例配置

```properties
# 服务版本元数据（必须配置）
spring.cloud.nacos.discovery.metadata.version=gray-v1
```

## 五、使用示例

### 场景1: 用户ID白名单灰度

**配置**:
```properties
gray.rules[0].name=user-white-list
gray.rules[0].serviceName=m1-consumer-service
gray.rules[0].ruleType=USER_ID
gray.rules[0].config.whiteList=user001,user002,user003
gray.rules[0].config.version=gray-v1
```

**请求**:
```bash
curl -H "X-User-Id: user001" http://localhost/api/m1-consumer/hello
```

### 场景2: 流量百分比灰度

**配置**:
```properties
gray.rules[1].name=percentage-10
gray.rules[1].serviceName=m1-consumer-service
gray.rules[1].ruleType=PERCENTAGE
gray.rules[1].config.percentage=10
gray.rules[1].config.version=gray-v2
```

**请求**:
```bash
curl http://localhost/api/m1-consumer/hello?userId=user123
```

### 场景3: 自定义标签灰度

**配置**:
```properties
gray.rules[2].name=tag-gray
gray.rules[2].serviceName=m1-consumer-service
gray.rules[2].ruleType=TAG
gray.rules[2].config.tag=beta
gray.rules[2].config.version=gray-v3
```

**请求**:
```bash
curl -H "X-Gray-Tag: beta" http://localhost/api/m1-consumer/hello
```

### 场景4: IP白名单灰度

**配置**:
```properties
gray.rules[3].name=ip-white-list
gray.rules[3].serviceName=*
gray.rules[3].ruleType=IP
gray.rules[3].config.whiteList=192.168.1.100,192.168.1.101
gray.rules[3].config.version=gray-v4
```

**请求**:
```bash
curl http://localhost/api/m1-consumer/hello
```

## 六、管理接口

### 1. 查看灰度配置
```http
GET http://localhost/gray/manage/config
```

### 2. 启用/禁用灰度
```http
POST http://localhost/gray/manage/toggle?enabled=true
```

### 3. 添加灰度规则
```http
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

### 4. 删除灰度规则
```http
DELETE http://localhost/gray/manage/rule/{ruleName}
```

### 5. 刷新灰度规则
```http
POST http://localhost/gray/manage/refresh
```

## 七、最佳实践

### 1. 灰度发布流程

```
1. 部署灰度版本服务实例（配置版本元数据）
   ↓
2. 配置IP白名单规则，内部测试
   ↓
3. 配置用户ID白名单，让特定用户体验
   ↓
4. 配置百分比灰度，逐步扩大流量（10% → 30% → 50% → 100%）
   ↓
5. 监控灰度版本性能和错误率
   ↓
6. 确认无误后，将灰度版本升级为稳定版本
   ↓
7. 下线旧版本服务实例
```

### 2. 版本命名规范

| 版本类型 | 版本命名 | 说明 |
|---------|---------|------|
| 稳定版本 | `stable` 或 `v1.0` | 生产环境稳定版本 |
| 灰度版本 | `gray-v1`, `gray-v2` | 灰度测试版本 |
| 金丝雀版本 | `canary-v1` | 金丝雀测试版本 |

### 3. 优先级设置建议

| 规则类型 | 优先级 | 说明 |
|---------|-------|------|
| IP白名单 | 5 | 最高优先级，内部测试 |
| 用户ID白名单 | 10 | 特定用户测试 |
| 百分比灰度 | 20 | 渐进式发布 |
| 标签灰度 | 30 | 低优先级，特定场景 |

### 4. 监控和告警

- 监控灰度版本的QPS、响应时间、错误率
- 设置告警阈值，及时发现问题
- 准备快速回滚方案

## 八、故障排查

### 问题1: 请求未路由到灰度版本

**检查步骤**:
1. 确认灰度已启用: `gray.enabled=true`
2. 检查灰度规则配置是否正确
3. 验证服务实例版本元数据: `spring.cloud.nacos.discovery.metadata.version`
4. 确认灰度规则中的version与服务实例元数据一致
5. 查看网关日志，确认是否匹配到灰度规则

### 问题2: 灰度规则不生效

**检查步骤**:
1. 确认规则的 `enabled` 为 `true`
2. 验证规则的 `serviceName` 是否匹配请求目标服务
3. 检查用户ID/IP/标签是否在白名单中
4. 验证百分比配置是否正确（0-100）
5. 查看规则优先级是否合理

### 问题3: 找不到灰度实例

**检查步骤**:
1. 确认灰度版本服务实例已启动并注册到Nacos
2. 验证服务实例的版本元数据配置
3. 检查Nacos服务发现配置
4. 查看负载均衡日志
5. 确认灰度规则中的版本名是否正确

## 九、扩展性

### 支持新的灰度策略

1. 在 `GrayConfig.GrayRuleType` 中添加新的规则类型
2. 在 `GrayReleaseFilter` 中实现新的匹配逻辑
3. 更新配置示例文档

### 集成第三方服务

- 可以集成Prometheus进行监控
- 可以集成ELK进行日志分析
- 可以集成Sentinel进行限流熔断

## 十、文件清单

```
my-gateway/
├── src/main/java/com/example/mygateway/
│   ├── config/
│   │   ├── GrayConfig.java                    # 灰度配置类
│   │   └── GrayLoadBalancerConfig.java        # 负载均衡配置
│   ├── context/
│   │   └── GrayContext.java                   # 灰度上下文
│   ├── filter/
│   │   └── GrayReleaseFilter.java            # 灰度过滤器
│   ├── loadbalancer/
│   │   ├── GrayLoadBalancer.java             # 灰度负载均衡器
│   │   └── LoadBalancerRequestContext.java    # 负载均衡上下文
│   ├── service/
│   │   └── GrayServiceManager.java           # 灰度服务管理器
│   └── controller/
│       └── GrayManageController.java         # 灰度管理控制器
├── src/main/resources/
│   └── application.properties                 # 网关配置
├── nacos-gray-config-example.properties      # Nacos配置示例
├── GRAY_RELEASE_GUIDE.md                     # 使用指南
└── GRAY_RELEASE_IMPLEMENTATION.md             # 实现文档

m1-consumer/
└── nacos-instance-config-example.properties   # 服务实例配置示例
```

## 十一、总结

本全链路灰度实现方案具有以下优势：

1. **灵活性**: 支持多种灰度策略，可灵活组合使用
2. **易用性**: 提供完整的管理接口和配置示例
3. **可靠性**: 支持降级机制，确保服务可用性
4. **可扩展性**: 易于扩展新的灰度策略
5. **可维护性**: 代码结构清晰，注释完善

通过本方案，可以安全、高效地进行微服务的灰度发布，降低发布风险，提高系统稳定性。
