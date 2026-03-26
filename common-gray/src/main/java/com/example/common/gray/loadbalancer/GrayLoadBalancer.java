package com.example.common.gray.loadbalancer;

import com.example.common.gray.constant.GrayConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 灰度负载均衡器
 * 根据灰度标记选择对应版本的服务实例
 */
@Slf4j
public class GrayLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
    private final String serviceId;
    private final Random random;

    public GrayLoadBalancer(
            ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
            String serviceId) {
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.serviceId = serviceId;
        this.random = ThreadLocalRandom.current();
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
                .getIfAvailable(() -> {
                    throw new IllegalStateException("No ServiceInstanceListSupplier available");
                });

        return supplier.get(request).next()
                .map(serviceInstances -> processInstanceResponse(serviceInstances, request));
    }

    private Response<ServiceInstance> processInstanceResponse(
            List<ServiceInstance> instances,
            Request request) {

        if (instances.isEmpty()) {
            log.warn("没有可用的服务实例: {}", serviceId);
            return new EmptyResponse();
        }

        // 从请求上下文中获取灰度版本
        String grayVersion = getGrayVersion(request);

        if (grayVersion == null) {
            // 没有灰度标记，使用普通负载均衡
            ServiceInstance instance = instances.get(random.nextInt(instances.size()));
            log.debug("普通负载均衡: service={}, instance={}", serviceId, instance.getInstanceId());
            return new DefaultResponse(instance);
        }

        // 过滤出匹配版本的服务实例
        List<ServiceInstance> grayInstances = instances.stream()
                .filter(instance -> {
                    String version = instance.getMetadata().get(GrayConstant.GRAY_VERSION_METADATA_KEY);
                    return grayVersion.equals(version);
                })
                .toList();

        if (grayInstances.isEmpty()) {
            log.warn("没有匹配灰度版本的服务实例: service={}, version={}", serviceId, grayVersion);
            // 降级到普通实例
            ServiceInstance instance = instances.get(random.nextInt(instances.size()));
            return new DefaultResponse(instance);
        }

        // 从灰度实例中选择一个
        ServiceInstance selectedInstance = grayInstances.get(random.nextInt(grayInstances.size()));
        log.info("灰度负载均衡: service={}, version={}, instance={}",
                serviceId, grayVersion, selectedInstance.getInstanceId());

        return new DefaultResponse(selectedInstance);
    }

    /**
     * 从请求中获取灰度版本
     */
    private String getGrayVersion(Request request) {
        // 尝试从RequestDataContext获取
        if (request.getContext() instanceof RequestDataContext dataContext) {
            String grayVersion = dataContext.getClientRequest().getHeaders().getFirst(GrayConstant.GRAY_VERSION_HEADER);
            if (grayVersion != null) {
                log.debug("从RequestDataContext获取灰度版本: {}", grayVersion);
                return grayVersion;
            }
        }

        return null;
    }
}
