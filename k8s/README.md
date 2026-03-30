# Kubernetes 部署说明

## 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                     Kubernetes Cluster                       │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                    cloud-demo namespace                  ││
│  │                                                          ││
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   ││
│  │  │ my-gateway   │  │ m1-consumer  │  │ m2-provider  │   ││
│  │  │   :8888      │──│   :8081      │──│   :8082      │   ││
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   ││
│  │         │                 │                 │           ││
│  │         ▼                 ▼                 ▼           ││
│  │  ┌──────────────┐  ┌──────────────────────────────┐    ││
│  │  │    Nacos     │  │      SkyWalking              │    ││
│  │  │    :8848     │  │  OAP(:11800) + UI(:8080)     │    ││
│  │  └──────────────┘  └──────────────────────────────┘    ││
│  │                                                      ││
│  │  ┌────────────────────────────────────────────────┐  ││
│  │  │           Elastic Stack                         │  ││
│  │  │  Filebeat(DaemonSet) → ES(:9200) → Kibana      │  ││
│  │  └────────────────────────────────────────────────┘  ││
│  └──────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

## 文件说明

| 文件 | 说明 |
|------|------|
| `namespace.yaml` | 命名空间定义 |
| `configmap.yaml` | 应用配置 (Nacos/SkyWalking 地址等) |
| `deployments.yaml` | 3个微服务 Deployment |
| `services.yaml` | 3个微服务 Service |
| `ingress.yaml` | Ingress 入口配置 |
| `nacos.yaml` | Nacos 配置中心 |
| `skywalking.yaml` | SkyWalking APM (ES + OAP + UI) |
| `elastic-stack.yaml` | 日志系统 (ES + Kibana + Filebeat) |
| `kustomization.yaml` | Kustomize 配置文件 |

## NodePort 访问地址

| 服务 | NodePort | 访问地址 |
|------|----------|---------|
| Nacos | 30848 | http://\<node-ip\>:30848/nacos |
| SkyWalking UI | 30080 | http://\<node-ip\>:30080 |
| Kibana | 30561 | http://\<node-ip\>:30561 |
| Gateway (Ingress) | 80/443 | http://cloud-demo.local |

## 前置要求

### 1. K8s 集群
- Kubernetes 1.20+
- kubectl 已配置

### 2. Ingress Controller
```bash
# 安装 nginx ingress controller (如果未安装)
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml
```

### 3. SkyWalking Agent (每个节点)
```bash
# 在每个 K8s 节点上执行
sudo mkdir -p /opt/skywalking-agent
sudo cp -r /path/to/skywalking-agent/* /opt/skywalking-agent/

# 或从官方下载
wget https://archive.apache.org/dist/skywalking/java-agent/9.6.0/apache-skywalking-java-agent-9.6.0.tgz
tar -xzf apache-skywalking-java-agent-9.6.0.tgz
sudo cp -r skywalking-agent/* /opt/skywalking-agent/
```

### 4. 本地 hosts 配置
```bash
# 添加到 /etc/hosts 或 C:\Windows\System32\drivers\etc\hosts
<node-ip> cloud-demo.local
```

## 部署步骤

### 方式1: 使用 Kustomize (推荐)

```bash
# 一键部署所有资源
kubectl apply -k ./k8s

# 查看部署状态
kubectl get all -n cloud-demo
```

### 方式2: 分步部署

```bash
# 1. 创建命名空间
kubectl apply -f k8s/namespace.yaml

# 2. 部署基础设施
kubectl apply -f k8s/nacos.yaml
kubectl apply -f k8s/skywalking.yaml
kubectl apply -f k8s/elastic-stack.yaml

# 3. 等待基础设施就绪
kubectl get pods -n cloud-demo -w

# 4. 部署应用服务
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployments.yaml
kubectl apply -f k8s/services.yaml
kubectl apply -f k8s/ingress.yaml
```

### 部署状态检查

```bash
# 查看所有资源
kubectl get all -n cloud-demo

# 查看 Pod 详情
kubectl describe pod <pod-name> -n cloud-demo

# 查看日志
kubectl logs -f <pod-name> -n cloud-demo

# 查看事件
kubectl get events -n cloud-demo --sort-by='.lastTimestamp'
```

## 配置修改

### 修改 Nacos 地址

编辑 `k8s/configmap.yaml`:

```yaml
data:
  NACOS_SERVER_ADDR: "nacos.cloud-demo.svc.cluster.local:8848"  # K8s 内部
  # 或外部地址
  # NACOS_SERVER_ADDR: "192.168.1.100:8848"
```

### 修改 SkyWalking 地址

编辑 `k8s/configmap.yaml`:

```yaml
data:
  SKYWALKING_COLLECTOR: "skywalking-oap.cloud-demo.svc.cluster.local:11800"
```

### 修改副本数

编辑 `k8s/deployments.yaml`:

```yaml
spec:
  replicas: 3  # 修改为所需副本数
```

### 修改资源限制

编辑 `k8s/deployments.yaml`:

```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "500m"
```

## 镜像管理

### 构建并推送镜像

```bash
# 设置镜像仓库地址
REGISTRY="your-registry.com"

# 构建镜像
docker build -t ${REGISTRY}/m1-consumer:latest ./m1-consumer
docker build -t ${REGISTRY}/m2-provider:latest ./m2-provider
docker build -t ${REGISTRY}/my-gateway:latest ./my-gateway

# 推送镜像
docker push ${REGISTRY}/m1-consumer:latest
docker push ${REGISTRY}/m2-provider:latest
docker push ${REGISTRY}/my-gateway:latest
```

### 更新镜像

```bash
# 更新 Deployment 镜像
kubectl set image deployment/m1-consumer m1-consumer=${REGISTRY}/m1-consumer:v1.0.0 -n cloud-demo
kubectl set image deployment/m2-provider m2-provider=${REGISTRY}/m2-provider:v1.0.0 -n cloud-demo
kubectl set image deployment/my-gateway my-gateway=${REGISTRY}/my-gateway:v1.0.0 -n cloud-demo
```

## 常用命令

```bash
# 扩缩容
kubectl scale deployment m1-consumer --replicas=3 -n cloud-demo

# 重启服务
kubectl rollout restart deployment m1-consumer -n cloud-demo

# 查看滚动更新状态
kubectl rollout status deployment m1-consumer -n cloud-demo

# 回滚
kubectl rollout undo deployment m1-consumer -n cloud-demo

# 进入容器
kubectl exec -it <pod-name> -n cloud-demo -- /bin/sh

# 端口转发
kubectl port-forward svc/my-gateway 8888:8888 -n cloud-demo

# 删除所有资源
kubectl delete -k ./k8s
```

## 故障排查

### Pod 一直 Pending

```bash
# 查看事件
kubectl describe pod <pod-name> -n cloud-demo

# 常见原因:
# 1. 资源不足 - 调整 resources 或添加节点
# 2. PVC 未绑定 - 检查存储类
# 3. 节点选择器不匹配 - 检查 nodeSelector
```

### 服务无法访问

```bash
# 检查 Service
kubectl get svc -n cloud-demo
kubectl describe svc <service-name> -n cloud-demo

# 检查 Endpoints
kubectl get endpoints -n cloud-demo

# 检查 Pod 标签
kubectl get pods -n cloud-demo --show-labels
```

### 日志查看

```bash
# 查看容器日志
kubectl logs -f <pod-name> -n cloud-demo

# 查看前一个容器日志 (如果重启过)
kubectl logs <pod-name> -n cloud-demo --previous

# 查看多容器 Pod 的日志
kubectl logs <pod-name> -c <container-name> -n cloud-demo
```

## 与 docker-compose 对比

| 功能 | docker-compose | Kubernetes |
|------|---------------|------------|
| 服务发现 | 容器名 | Service (DNS) |
| 配置管理 | 环境变量 | ConfigMap |
| 副本数 | 单实例 | Deployment (可配置) |
| 健康检查 | 可选 | livenessProbe/readinessProbe |
| 日志采集 | 挂载宿主机目录 | Filebeat DaemonSet |
| 入口路由 | 端口映射 | Ingress |
| 负载均衡 | 内置 | Service |
| 滚动更新 | 手动 | 自动 |
| 回滚 | 手动 | 自动 |

## Jenkins CI/CD

使用 `Jenkinsfile-k8s` 进行自动化部署:

```groovy
// Jenkinsfile-k8s 关键配置
environment {
    REGISTRY = 'localhost:5000'  // 修改为你的镜像仓库
    K8S_NAMESPACE = 'cloud-demo'
}
```

Jenkins Pipeline 流程:
1. Checkout - 拉取代码
2. Build - Maven 构建
3. Build Images - 构建镜像
4. Push Images - 推送到仓库
5. Deploy to K8s - 部署到 Kubernetes
6. Health Check - 健康检查
