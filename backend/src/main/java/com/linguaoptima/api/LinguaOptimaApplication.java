package com.linguaoptima.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @file LinguaOptimaApplication.java
 * @brief Spring Boot entry point for the Lingua Optima backend application.
 */
@SpringBootApplication
public class LinguaOptimaApplication {

    /**
     * @brief Main method initiating the Spring ApplicationContext and web server.
     * @param args Command line arguments passed at application startup.
     */
    public static void main(String[] args) {
        SpringApplication.run(LinguaOptimaApplication.class, args);
    }
}
