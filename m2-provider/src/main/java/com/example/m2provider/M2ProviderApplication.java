package com.example.m2provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class M2ProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(M2ProviderApplication.class, args);
    }

}
