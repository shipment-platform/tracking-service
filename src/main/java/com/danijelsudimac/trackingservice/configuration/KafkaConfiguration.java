package com.danijelsudimac.trackingservice.configuration;

import com.danijelsudimac.trackingservice.service.TrackingMetrics;
import com.google.protobuf.GeneratedMessage;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Configuration
@Slf4j
@ConditionalOnProperty(
        value = "app.kafka-consumer.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class KafkaConfiguration {

    private static final String ERROR_LOG_MESSAGE = "Error processing record with key {}: {} with message {}. Sending to {} topic";
    private static final Set<Class<? extends Throwable>> INVALID_MESSAGE_EXCEPTIONS = Set.of(
            DeserializationException.class,
            SerializationException.class,
            IllegalArgumentException.class
    ); //poison pill exceptions

    private final String topicDtl;
    private final String topicInvalidMessages;
    private final TrackingMetrics trackingMetrics;

    public KafkaConfiguration(@Value("${application.kafka.shipment-topic-dlt}") String topicDtl,
                              @Value("${application.kafka.topic-invalid-messages}") String topicInvalidMessages,
                              TrackingMetrics trackingMetrics) {
        this.topicDtl = topicDtl;
        this.topicInvalidMessages = topicInvalidMessages;
        this.trackingMetrics = trackingMetrics;
    }

    @Bean
    public ProducerFactory<Object, Object> producerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaProducerFactory<>(
                kafkaProperties.buildProducerProperties());
    }

    @Bean
    ProducerFactory<String, byte[]> poisonProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, byte[]> poisonTemplate(ProducerFactory<String, byte[]> poisonProducerFactory) {
        return new KafkaTemplate<>(poisonProducerFactory);
    }

    @Bean
    public KafkaTemplate<Object, Object> kafkaTemplate(ProducerFactory<Object, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, byte[]> poisonTemplate,
                                            KafkaTemplate<Object, Object> kafkaTemplate) {

        var recoverer =
                new DeadLetterPublishingRecoverer(Map.of(GeneratedMessage.class, kafkaTemplate, byte[].class, poisonTemplate),
                        (record, ex) -> {
                            Throwable rootException = Optional.ofNullable(ex.getCause()).orElse(ex);
                            var topic = resolveTopic(rootException);
                            log.warn(ERROR_LOG_MESSAGE, record.key(), rootException.getClass(), rootException.getMessage(), topic);
                            trackingMetrics.incrementMessageInDlt();
                            return new TopicPartition(topic, record.partition());
                        });
        var errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
        errorHandler.setCommitRecovered(true);
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                DeserializationException.class,
                ValidationException.class
        );

        return errorHandler;
    }

    private String resolveTopic(Throwable ex) {
        return INVALID_MESSAGE_EXCEPTIONS.stream()
                .anyMatch(clazz -> clazz.isInstance(ex))
                ? topicInvalidMessages
                : topicDtl;
    }
}