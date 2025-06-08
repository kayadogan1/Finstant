package com.dogankaya.KafkaConsumer1_PostgreSQL_OpenSearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * Spring Boot application entry point for the KafkaConsumer1_PostgreSQL_OpenSearch project.
 * <p>
 * This application configures Spring Boot auto-configuration and scans the "rate" package
 * for JPA entities.
 * </p>
 */
@SpringBootApplication
@EntityScan(basePackages = {"rate"})
public class KafkaConsumer1PostgreSqlOpenSearchApplication {

	/**
	 * Main method to launch the Spring Boot application.
	 *
	 * @param args command-line arguments passed during application startup.
	 */
	public static void main(String[] args) {
		SpringApplication.run(KafkaConsumer1PostgreSqlOpenSearchApplication.class, args);
	}
}
