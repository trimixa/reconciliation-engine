package com.maang.reconciliation.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ConsumerApplication {

    public static void main(String[] args) {
        // Fixes "FATAL: invalid value for parameter TimeZone: Asia/Calcutta"
        // by forcing the JVM to use the modern "Asia/Kolkata" timezone identifier
        // which the latest PostgreSQL versions require.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(ConsumerApplication.class, args);
    }
}