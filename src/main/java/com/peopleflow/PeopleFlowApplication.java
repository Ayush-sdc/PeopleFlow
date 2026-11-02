package com.peopleflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * PeopleFlow - SaaS Employee & Department Management System
 * Entry point for the Spring Boot application.
 */
@SpringBootApplication
@EnableJpaAuditing
public class PeopleFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(PeopleFlowApplication.class, args);
    }
}