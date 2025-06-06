package com.dogankaya.KafkaConsumer1_PostgreSQL_OpenSearch.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import rate.RateDocument;


public interface RateSearchRepository extends ElasticsearchRepository<RateDocument, Integer> {
}
