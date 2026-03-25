# 全链路灰度快速开始

## 快速体验全链路灰度功能

### 第一步：启动Nacos

```bash
# 下载Nacos并启动
cd nacos/bin
./startup.sh -m standalone
```

### 第二步：配置灰度规则

登录Nacos控制台（http://localhost:8848/nacos），创建配置：

**配置信息**:
- Data ID: `my-gateway-service-dev.properties`
- Group: `DEFAULT_GROUP`
- 配置格式: `Properties`

**配置内容**:
```properties
# 启用灰度
gray.enabled=true

# 用户ID白名单灰度
gray.rules[0].name=user-test
gray.rules[0].serviceName=m1-consumer-service
gray.rules[0].ruleType=USER_ID
gray.rules[0].config.whiteList=test001,test002,test003
gray.rules[0].config.version=gray-v1
gray.rules[0].priority=10
gray.rules[0].enabled=true
```

### 第三步：启动稳定版本服务实例

修改 `m1-consumer/src/main/resources/application.properties`:
```properties
server.port=8081
spring.application.name=m1-consumer-service
spring.cloud.nacos.discovery.metadata.version=stable
```

启动服务:
```bash
mvn spring-boot:run -pl m1-consumer
```

### 第四步：启动灰度版本服务实例

修改 `m1-consumer/src/main/resources/application.properties`:
```properties
server.port=8082
spring.application.name=m1-consumer-service
spring.cloud.nacos.discovery.metadata.version=gray-v1
```

启动服务:
```bash
mvn spring-boot:run -pl m1-consumer
```

### 第五步：启动网关

```bash
mvn spring-boot:run -pl my-gateway
```

### 第六步：测试灰度功能

#### 测试1：普通用户请求（路由到稳定版本）
```bash
curl http://localhost/api/m1-consumer/hello?userId=user999
```

**预期结果**: 请求被路由到端口8081的稳定版本

#### 测试2：灰度用户请求（路由到灰度版本）
```bash
curl http://localhost/api/m1-consumer/hello?userId=test001
```

**预期结果**: 请求被路由到端口8082的灰度版本

#### 测试3：查看灰度配置
```bash
curl http://localhost/gray/manage/config
```

#### 测试4：禁用灰度
```bash
curl -X POST "http://localhost/gray/manage/toggle?enabled=false"
```

**预期结果**: 所有请求都路由到稳定版本

### 第七步：观察日志

查看网关日志，会看到灰度请求的详细信息：

```
灰度请求: service=m1-consumer-service, version=gray-v1, userId=test001, ip=127.0.0.1, rule=user-test
```

查看负载均衡日志，会看到实例选择信息：

```
灰度负载均衡: service=m1-consumer-service, version=gray-v1, instance=192.168.1.1:8082
```

## 其他灰度策略示例

### 百分比灰度

修改Nacos配置:
```properties
# 20%流量灰度
gray.rules[1].name=percentage-20
gray.rules[1].serviceName=m1-consumer-service
gray.rules[1].ruleType=PERCENTAGE
gray.rules[1].config.percentage=20
gray.rules[1].config.version=gray-v2
gray.rules[1].priority=20
gray.rules[1].enabled=true
```

测试：
```bash
curl http://localhost/api/m1-consumer/hello?userId=user123
```

### 标签灰度

修改Nacos配置:
```properties
# 标签灰度
gray.rules[2].name=tag-beta
gray.rules[2].serviceName=m1-consumer-service
gray.rules[2].ruleType=TAG
gray.rules[2].config.tag=beta
gray.rules[2].config.version=gray-v3
gray.rules[2].priority=30
gray.rules[2].enabled=true
```

测试：
```bash
curl -H "X-Gray-Tag: beta" http://localhost/api/m1-consumer/hello
```

## 常见问题

### Q1: 如何确认服务实例的版本配置正确？

查看Nacos服务列表，点击服务详情，查看实例的元数据：
- 应该有 `version: stable` 或 `version: gray-v1`

### Q2: 灰度规则如何动态更新？

在Nacos中修改配置后，无需重启，网关会自动刷新配置。

### Q3: 如何快速回滚？

方式1：在Nacos中修改 `gray.enabled=false`
方式2：调用接口 `POST http://localhost/gray/manage/toggle?enabled=false`

### Q4: 多个灰度规则如何匹配？

按优先级从小到大匹配，找到第一个匹配的规则即停止。

### Q5: 如何查看某个请求被路由到哪个实例？

查看网关日志中的灰度负载均衡日志：
```
灰度负载均衡: service=m1-consumer-service, version=gray-v1, instance=192.168.1.1:8082
```

## 下一步

- 阅读完整使用指南: `GRAY_RELEASE_GUIDE.md`
- 了解实现原理: `GRAY_RELEASE_IMPLEMENTATION.md`
- 查看配置示例: `nacos-gray-config-example.properties`
