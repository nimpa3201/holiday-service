package com.planitsquare.holidayservice.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {

    @Bean(name = "holidayExecutor")
    public Executor holidayExecutor() {
        return Executors.newFixedThreadPool(10);
    }
}