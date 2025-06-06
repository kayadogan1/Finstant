package com.dogankaya.FinanStream.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import rate.RateDto;

@Service
public class KafkaProducer {
    private final KafkaTemplate<String, RateDto> kafkaTemplate;
    public KafkaProducer(KafkaTemplate<String, RateDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    public void sendRate(String topic, RateDto rateDto) {
        kafkaTemplate.send(topic, rateDto);
    }}
