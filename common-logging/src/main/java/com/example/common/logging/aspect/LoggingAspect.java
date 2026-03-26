package com.example.common.logging.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * 方法日志切面
 * 记录Controller/Service层方法的入参、出参和耗时
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /**
     * 切入点：所有Controller层的public方法
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restControllerMethods() {}

    /**
     * 切入点：所有Service层的public方法
     */
    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceMethods() {}

    /**
     * 切入点：所有Repository层的public方法
     */
    @Pointcut("within(@org.springframework.stereotype.Repository *)")
    public void repositoryMethods() {}

    /**
     * 组合切入点：Controller + Service + Repository
     */
    @Pointcut("restControllerMethods() || serviceMethods() || repositoryMethods()")
    public void applicationLayerMethods() {}

    @Around("applicationLayerMethods()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();
        Object[] args = joinPoint.getArgs();

        // 记录方法开始
        if (log.isDebugEnabled()) {
            log.debug("Method start - {}.{} with arguments: {}", className, methodName, args);
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            // 记录异常
            log.error("Method error - {}.{} with arguments: {}, error: {}",
                      className, methodName, args, throwable.getMessage(), throwable);
            throw throwable;
        } finally {
            stopWatch.stop();
            long duration = stopWatch.getTotalTimeMillis();
            // 记录方法结束
            if (log.isDebugEnabled()) {
                log.debug("Method completed - {}.{} took {}ms, result: {}",
                          className, methodName, duration, result);
            } else if (log.isInfoEnabled() && duration > 100) {
                // 如果方法执行时间超过100ms，记录info日志
                log.info("Slow method - {}.{} took {}ms", className, methodName, duration);
            }
        }
    }
}