/**
 * @file SchedulingConfig.java
 * @brief Enables Spring declarative background task scheduling support.
 */
package com.linguaoptima.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @brief Enables Spring declarative background task scheduling support.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
