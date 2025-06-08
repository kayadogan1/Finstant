package com.dogankaya.KafkaConsumer1_PostgreSQL_OpenSearch.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rate.Rate;

/**
 * Repository interface for {@link Rate} entity.
 * <p>
 * Extends {@link JpaRepository} to provide CRUD operations
 * and pagination for {@link Rate} entities with primary key of type {@link Integer}.
 * </p>
 */
public interface RateRepository extends JpaRepository<Rate, Integer> {
}
