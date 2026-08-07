package com.shophub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableJpaRepositories(basePackages = "com.shophub.repository")
public class ShopHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopHubApplication.class, args);
    }
}
