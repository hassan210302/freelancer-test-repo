package com.respiroc.webapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.respiroc"], exclude = [UserDetailsServiceAutoConfiguration::class])
class WebApplication

fun main(args: Array<String>) {
    runApplication<WebApplication>(*args)
}