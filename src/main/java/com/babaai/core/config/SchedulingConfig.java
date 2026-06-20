package com.babaai.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables {@code @Scheduled} jobs (e.g. the daily suggestion generator, 869dr0a4d). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
