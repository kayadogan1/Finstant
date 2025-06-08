package com.dogankaya.KafkaConsumer1_PostgreSQL_OpenSearch.services;

import com.dogankaya.KafkaConsumer1_PostgreSQL_OpenSearch.repository.RateRepository;
import com.dogankaya.KafkaConsumer1_PostgreSQL_OpenSearch.repository.RateSearchRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import rate.RateDocument;
import rate.RateDto;
import rate.Rate;

import java.time.format.DateTimeFormatter;

/**
 * Service class responsible for consuming RateDto messages from Kafka,
 * persisting them into PostgreSQL database, and indexing into OpenSearch.
 * <p>
 * This service listens to the Kafka topic "rate-topic" with concurrency level 6,
 * converts incoming {@link RateDto} objects into {@link Rate} and {@link RateDocument}
 * entities, saves them to respective repositories, and logs the operations.
 * </p>
 * <p>
 * Scheduling is enabled to support any future scheduled tasks if needed.
 * </p>
 */
@Service
@EnableScheduling
public class RateConsumerService {
    private final Logger logger = LogManager.getLogger();
    private final RateRepository rateRepository;
    private final RateSearchRepository rateSearchRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Constructor with dependencies injected by Spring.
     *
     * @param rateRepository        Repository for saving Rate entities in PostgreSQL.
     * @param rateSearchRepository  Repository for indexing RateDocument entities in OpenSearch.
     */
    public RateConsumerService(RateRepository rateRepository, RateSearchRepository rateSearchRepository) {
        this.rateRepository = rateRepository;
        this.rateSearchRepository = rateSearchRepository;
    }

    /**
     * Kafka listener method that consumes {@link RateDto} messages from the "rate-topic".
     * Converts the DTO to database and search document entities, saves them,
     * and logs the operation.
     *
     * @param rateDto the incoming RateDto message from Kafka.
     */
    @KafkaListener(topics = "rate-topic", groupId = "rate-group", concurrency = "6")
    public void consumeRate(RateDto rateDto) {
        Rate rate = Rate.builder()
                .rateName(rateDto.getRateName())
                .ask(rateDto.getAsk())
                .bid(rateDto.getBid())
                .rateUpdateTime(rateDto.getRateUpdateTime())
                .dbUpdateTime(rateDto.getRateUpdateTime())
                .build();
        rateRepository.save(rate);

        RateDocument rateDocument = RateDocument.builder()
                .id(rate.getId())
                .rateName(rateDto.getRateName())
                .ask(rateDto.getAsk())
                .bid(rateDto.getBid())
                .rateUpdateTime(rateDto.getRateUpdateTime().format(formatter))
                .dbUpdateTime(rateDto.getRateUpdateTime().format(formatter))
                .build();
        rateSearchRepository.save(rateDocument);

        logger.info("Rate:{} saved and sent to OpenSearch", rate.getRateName());
    }
}
