package com.nnipa.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.nnipa.gateway.config.ApiGatewayProperties;

@SpringBootApplication
@EnableConfigurationProperties(ApiGatewayProperties.class)
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}