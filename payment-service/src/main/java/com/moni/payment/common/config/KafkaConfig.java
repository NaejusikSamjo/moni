package com.moni.payment.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaConfig {

    public static final String TOPIC_SUBSCRIPTION_SUCCEEDED = "payment.subscription.succeeded";
    public static final String TOPIC_SUBSCRIPTION_CANCELLED = "payment.subscription.cancelled";
    public static final String TOPIC_BILLING_FAILED = "payment.billing.failed";

    private static final int TOPIC_PARTITIONS = 3;
    private static final int TOPIC_REPLICAS = 1;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public NewTopic subscriptionSucceededTopic() {
        return TopicBuilder.name(TOPIC_SUBSCRIPTION_SUCCEEDED)
                .partitions(TOPIC_PARTITIONS)
                .replicas(TOPIC_REPLICAS)
                .build();
    }

    @Bean
    public NewTopic subscriptionCancelledTopic() {
        return TopicBuilder.name(TOPIC_SUBSCRIPTION_CANCELLED)
                .partitions(TOPIC_PARTITIONS)
                .replicas(TOPIC_REPLICAS)
                .build();
    }

    @Bean
    public NewTopic billingFailedTopic() {
        return TopicBuilder.name(TOPIC_BILLING_FAILED)
                .partitions(TOPIC_PARTITIONS)
                .replicas(TOPIC_REPLICAS)
                .build();
    }
}
