package com.dogankaya.FinanStream.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import rate.RateDto;

/**
 * Service class responsible for producing and sending messages to Kafka topics.
 *
 * <p>This class uses {@link KafkaTemplate} to send {@link RateDto} messages to specified Kafka topics.</p>
 */
@Service
public class KafkaProducer {

    private final KafkaTemplate<String, RateDto> kafkaTemplate;

    /**
     * Constructs a new KafkaProducer with the given KafkaTemplate.
     *
     * @param kafkaTemplate the {@link KafkaTemplate} used to send messages to Kafka
     */
    public KafkaProducer(KafkaTemplate<String, RateDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends a {@link RateDto} message to the specified Kafka topic.
     *
     * @param topic the name of the Kafka topic to send the message to
     * @param rateDto the {@link RateDto} message object to send
     */
    public void sendRate(String topic, RateDto rateDto) {
        kafkaTemplate.send(topic, rateDto);
    }
}
