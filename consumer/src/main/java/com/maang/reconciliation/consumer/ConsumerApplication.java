package com.maang.reconciliation.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.TimeZone; // <-- Add this import

@SpringBootApplication
public class ConsumerApplication {

    public static void main(String[] args) {
        // Force Java to use the modern timezone name before it talks to Postgres
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

        SpringApplication.run(ConsumerApplication.class, args);
    }
}