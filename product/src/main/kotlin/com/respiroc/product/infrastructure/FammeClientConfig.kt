package com.respiroc.product.infrastructure.famme

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class FammeClientConfig {

    @Bean
    fun fammeRestClient(): RestClient {
        return RestClient.builder()
            .baseUrl("https://famme.no")
            .build()
    }
}