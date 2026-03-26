package com.example.mygateway.filter;

import com.example.common.gray.config.GrayConfig;
import com.example.common.gray.context.GrayContext;
import com.example.common.gray.constant.GrayConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;

/**
 * 灰度发布过滤器
 * 在网关层对请求进行灰度标记
 */
@Slf4j
@Component
public class GrayReleaseFilter implements GlobalFilter, Ordered {
    
    private final GrayConfig grayConfig;
    private final Random random = new Random();
    
    public GrayReleaseFilter(GrayConfig grayConfig) {
        this.grayConfig = grayConfig;
    }
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 如果灰度未启用，直接放行
        if (!Boolean.TRUE.equals(grayConfig.getEnabled())) {
            return chain.filter(exchange);
        }

        //取路由id
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = route.getId();
        String appid = (String)route.getMetadata().get("appid");

        // 获取目标服务名
        String serviceName = getServiceName(exchange);
        if (serviceName == null) {
            return chain.filter(exchange);
        }
        
        // 构建灰度上下文
        GrayContext grayContext = buildGrayContext(exchange, serviceName);
        
        // 匹配灰度规则
        matchGrayRule(grayContext);
        
        // 如果匹配到灰度规则，添加灰度标记头
        if (Boolean.TRUE.equals(grayContext.getIsGray())) {
            exchange = addGrayHeaders(exchange, grayContext);
            log.info("灰度请求: service={}, version={}, userId={}, ip={}, rule={}",
                    serviceName, grayContext.getVersion(), grayContext.getUserId(),
                    grayContext.getClientIp(), grayContext.getRuleName());
        }
        
        return chain.filter(exchange);
    }
    
    /**
     * 获取目标服务名
     * 从请求路径中解析服务名
     */
    private String getServiceName(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        // 假设路径格式为 /api/{serviceName}/...
        String[] parts = path.split("/");
        if (parts.length >= 3) {
            return parts[2];
        }
        return null;
    }
    
    /**
     * 构建灰度上下文
     */
    private GrayContext buildGrayContext(ServerWebExchange exchange, String serviceName) {
        GrayContext context = GrayContext.create()
                .setServiceName(serviceName)
                .setIsGray(false);
        
        // 获取用户ID（从请求头或参数）
        String userId = getUserId(exchange);
        context.setUserId(userId);
        
        // 获取客户端IP
        String clientIp = getClientIp(exchange);
        context.setClientIp(clientIp);
        
        // 获取自定义标签
        String tag = getTag(exchange);
        context.setTag(tag);
        
        return context;
    }
    
    /**
     * 匹配灰度规则
     */
    private void matchGrayRule(GrayContext context) {
        if (grayConfig.getRules() == null) {
            return;
        }
        
        // 按优先级排序规则
        List<GrayConfig.GrayRule> sortedRules = grayConfig.getRules().stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getEnabled()))
                .sorted((r1, r2) -> r1.getPriority().compareTo(r2.getPriority()))
                .toList();
        
        for (GrayConfig.GrayRule rule : sortedRules) {
            // 检查服务名是否匹配
            if (!isServiceMatch(rule.getServiceName(), context.getServiceName())) {
                continue;
            }
            
            // 根据规则类型判断是否匹配
            boolean matched = switch (rule.getRuleType()) {
                case USER_ID -> matchUserIdRule(rule, context);
                case PERCENTAGE -> matchPercentageRule(rule, context);
                case TAG -> matchTagRule(rule, context);
                case IP -> matchIpRule(rule, context);
            };
            
            if (matched) {
                context.setIsGray(true)
                        .setVersion(rule.getConfig().get("version"))
                        .setRuleName(rule.getName());
                break;
            }
        }
    }
    
    /**
     * 服务名匹配（支持通配符）
     */
    private boolean isServiceMatch(String pattern, String serviceName) {
        if (serviceName == null) {
            return false;
        }
        if (pattern.equals("*")) {
            return true;
        }
        return pattern.equals(serviceName);
    }
    
    /**
     * 匹配用户ID规则
     */
    private boolean matchUserIdRule(GrayConfig.GrayRule rule, GrayContext context) {
        if (context.getUserId() == null) {
            return false;
        }
        String whiteList = rule.getConfig().get("whiteList");
        if (whiteList == null) {
            return false;
        }
        String[] userIds = whiteList.split(",");
        for (String id : userIds) {
            if (id.trim().equals(context.getUserId())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 匹配百分比规则
     */
    private boolean matchPercentageRule(GrayConfig.GrayRule rule, GrayContext context) {
        String percentageStr = rule.getConfig().get("percentage");
        if (percentageStr == null) {
            return false;
        }
        try {
            int percentage = Integer.parseInt(percentageStr);
            if (percentage <= 0) {
                return false;
            }
            if (percentage >= 100) {
                return true;
            }
            // 根据用户ID或随机数计算hash
            int hash = calculateHash(context.getUserId());
            return (hash % 100) < percentage;
        } catch (NumberFormatException e) {
            log.error("百分比配置格式错误: {}", percentageStr, e);
            return false;
        }
    }
    
    /**
     * 匹配标签规则
     */
    private boolean matchTagRule(GrayConfig.GrayRule rule, GrayContext context) {
        if (context.getTag() == null) {
            return false;
        }
        String expectedTag = rule.getConfig().get("tag");
        return context.getTag().equals(expectedTag);
    }
    
    /**
     * 匹配IP规则
     */
    private boolean matchIpRule(GrayConfig.GrayRule rule, GrayContext context) {
        if (context.getClientIp() == null) {
            return false;
        }
        String whiteList = rule.getConfig().get("whiteList");
        if (whiteList == null) {
            return false;
        }
        String[] ips = whiteList.split(",");
        for (String ip : ips) {
            if (ip.trim().equals(context.getClientIp())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 计算hash值，用于百分比灰度
     */
    private int calculateHash(String key) {
        if (key == null) {
            return random.nextInt(100);
        }
        int hash = 0;
        for (int i = 0; i < key.length(); i++) {
            hash = 31 * hash + key.charAt(i);
        }
        return Math.abs(hash);
    }
    
    /**
     * 添加灰度标记头
     */
    private ServerWebExchange addGrayHeaders(ServerWebExchange exchange, GrayContext context) {
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();
        
        // 添加必需的灰度头部
        requestBuilder.header(GrayConstant.GRAY_VERSION_HEADER, context.getVersion());
        requestBuilder.header(GrayConstant.GRAY_RULE_HEADER, context.getRuleName());
        
        // 添加可选的灰度头部
        if (context.getUserId() != null) {
            requestBuilder.header(GrayConstant.GRAY_USER_ID_HEADER, context.getUserId());
        }
        if (context.getTag() != null) {
            requestBuilder.header(GrayConstant.GRAY_TAG_HEADER, context.getTag());
        }
        
        ServerHttpRequest newRequest = requestBuilder.build();
        
        // 创建并返回新的 exchange
        return exchange.mutate().request(newRequest).build();
    }
    
    /**
     * 获取用户ID
     */
    private String getUserId(ServerWebExchange exchange) {
        // 从请求头获取
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId != null) {
            return userId;
        }
        // 从请求参数获取
        userId = exchange.getRequest().getQueryParams().getFirst("userId");
        if (userId != null) {
            return userId;
        }
        return null;
    }
    
    /**
     * 获取客户端IP
     */
    private String getClientIp(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null) {
            InetAddress address = remoteAddress.getAddress();
            if (address != null) {
                return address.getHostAddress();
            }
        }
        return null;
    }
    
    /**
     * 获取自定义标签
     */
    private String getTag(ServerWebExchange exchange) {
        String tag = exchange.getRequest().getHeaders().getFirst("X-Gray-Tag");
        if (tag != null) {
            return tag;
        }
        tag = exchange.getRequest().getQueryParams().getFirst("grayTag");
        if (tag != null) {
            return tag;
        }
        return null;
    }
    
    @Override
    public int getOrder() {
        // 在路由之后，在负载均衡之前执行
//        return Ordered.LOWEST_PRECEDENCE - 1;
        return Ordered.HIGHEST_PRECEDENCE; // 最先执行
    }
}
