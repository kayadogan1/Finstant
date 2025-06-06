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

@Service
@EnableScheduling
public class RateConsumerService {
    private final Logger logger = LogManager.getLogger();
    private final RateRepository rateRepository;
    private final RateSearchRepository rateSearchRepository;

    public RateConsumerService(RateRepository rateRepository, RateSearchRepository rateSearchRepository) {
        this.rateRepository = rateRepository;
        this.rateSearchRepository = rateSearchRepository;
    }

    @KafkaListener(topics = "rate-topic", groupId = "rate-group")
    public void consumeRate(RateDto rateDto) {
        Rate rate = Rate.builder()
                .rateName(rateDto.getRateName())
                .ask(rateDto.getAsk())
                .bid(rateDto.getBid())
                .rateUpdateTime(rateDto.getRateUpdateTime())
                .build();
        rateRepository.save(rate);

        RateDocument rateDocument = RateDocument.builder()
                .id(rate.getId())
                .rateName(rateDto.getRateName())
                .ask(rateDto.getAsk())
                .bid(rateDto.getBid())
                .rateUpdateTime(rateDto.getRateUpdateTime())
                .build();
        rateSearchRepository.save(rateDocument);
        logger.info("Rate:{} saved and sended opensearch", rate.getRateName());
    }
}
