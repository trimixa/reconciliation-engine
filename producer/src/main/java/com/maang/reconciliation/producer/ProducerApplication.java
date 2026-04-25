package com.maang.reconciliation.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProducerApplication {

	private static final Logger logger = LoggerFactory.getLogger(ProducerApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(ProducerApplication.class, args);
		logger.info("The Spring Boot engine is officially running!");
	}
}