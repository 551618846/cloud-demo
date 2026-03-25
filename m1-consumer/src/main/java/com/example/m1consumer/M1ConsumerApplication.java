package com.example.m1consumer;

import com.example.m1consumer.config.GrayLoadBalancerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
@LoadBalancerClients(defaultConfiguration = GrayLoadBalancerConfig.class)
public class M1ConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(M1ConsumerApplication.class, args);
    }

}
