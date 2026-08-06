package com.nhnacademy.ruleengine.global.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;


@Configuration
public class RestClientConfig {

    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @Profile("prod")
    public RestClient restClient(@LoadBalanced RestClient.Builder builder) {
        return builder.build();
    }

    @Bean
    @Profile("!prod")
    public RestClient devRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}


