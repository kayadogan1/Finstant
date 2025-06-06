package com.dogankaya.KafkaConsumer1_PostgreSQL_OpenSearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = {"rate"})
public class KafkaConsumer1PostgreSqlOpenSearchApplication {

    public static void main(String[] args) {
		SpringApplication.run(KafkaConsumer1PostgreSqlOpenSearchApplication.class, args);
	}
}
