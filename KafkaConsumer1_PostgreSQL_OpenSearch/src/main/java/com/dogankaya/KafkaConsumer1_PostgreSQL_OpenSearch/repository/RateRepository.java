package com.dogankaya.KafkaConsumer1_PostgreSQL_OpenSearch.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rate.Rate;

public interface RateRepository extends JpaRepository<Rate, Integer> {
}
