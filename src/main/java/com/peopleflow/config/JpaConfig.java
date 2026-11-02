package com.peopleflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Configuration.
 * Isolating @EnableJpaAuditing in a dedicated @Configuration class avoids
 * slice test context loading failures in @WebMvcTest.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}