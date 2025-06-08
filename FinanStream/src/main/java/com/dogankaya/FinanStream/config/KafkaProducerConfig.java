package com.dogankaya.FinanStream.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import rate.RateDto;

import java.util.HashMap;
import java.util.Map;

/**
 * KafkaProducerConfig is a Spring configuration class that sets up the Kafka producer
 * for sending {@link RateDto} messages.
 *
 * <p>This configuration defines the necessary producer factory and Kafka template
 * beans required to produce messages to a Kafka topic.</p>
 *
 * <p>It uses {@link StringSerializer} for the key and {@link JsonSerializer} for
 * serializing the {@link RateDto} objects as message values.</p>
 */
@Configuration
public class KafkaProducerConfig {

    /**
     * Kafka bootstrap servers address, injected from application properties.
     */
    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;

    /**
     * Creates a Kafka {@link ProducerFactory} bean configured for sending messages
     * with String keys and {@link RateDto} values serialized as JSON.
     *
     * @return a configured {@link ProducerFactory} instance
     */
    @Bean
    public ProducerFactory<String, RateDto> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Creates a Kafka {@link KafkaTemplate} bean used to send messages with String keys
     * and {@link RateDto} values.
     *
     * @return a configured {@link KafkaTemplate} instance
     */
    @Bean
    public KafkaTemplate<String, RateDto> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
