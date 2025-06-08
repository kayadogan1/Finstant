package com.dogankaya.KafkaConsumer1_PostgreSQL_OpenSearch.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import rate.RateDto;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for Kafka consumer setup.
 * <p>
 * This class sets up the Kafka consumer factory and listener container factory
 * with necessary configurations such as bootstrap servers, group ID, and deserializers.
 * It is responsible for consuming messages of type {@link RateDto} from Kafka topics.
 * </p>
 */
@Configuration
@ConfigurationProperties
public class KafkaConsumerConfig {

    /**
     * Kafka bootstrap servers URL(s), injected from application properties.
     */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Creates a {@link ConsumerFactory} bean configured to deserialize keys as Strings
     * and values as JSON objects of type {@link RateDto}.
     *
     * @return a configured {@link ConsumerFactory} for Kafka consumers.
     */
    @Bean
    public ConsumerFactory<String, RateDto> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "rate-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
                new JsonDeserializer<>(RateDto.class));
    }

    /**
     * Creates a {@link ConcurrentKafkaListenerContainerFactory} bean that uses the
     * {@link ConsumerFactory} to create Kafka listener containers.
     * <p>
     * This factory supports concurrent consumption of Kafka messages and
     * is used by Spring's {@code @KafkaListener} annotations.
     * </p>
     *
     * @return a configured {@link ConcurrentKafkaListenerContainerFactory}.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RateDto> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, RateDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
