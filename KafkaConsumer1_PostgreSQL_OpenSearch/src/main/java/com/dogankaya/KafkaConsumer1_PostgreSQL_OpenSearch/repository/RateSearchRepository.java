package com.dogankaya.KafkaConsumer1_PostgreSQL_OpenSearch.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import rate.RateDocument;

/**
 * Elasticsearch repository interface for {@link RateDocument} entity.
 * <p>
 * Extends {@link ElasticsearchRepository} to provide CRUD operations,
 * search, and pagination capabilities for {@link RateDocument} entities
 * with primary key of type {@link Integer}.
 * </p>
 */
public interface RateSearchRepository extends ElasticsearchRepository<RateDocument, Integer> {
}
