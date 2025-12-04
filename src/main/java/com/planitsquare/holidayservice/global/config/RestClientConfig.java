package com.planitsquare.holidayservice.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient nagerRestClient() {
        return RestClient.builder()
            .baseUrl("https://date.nager.at/api/v3")
            .build();
    }
}