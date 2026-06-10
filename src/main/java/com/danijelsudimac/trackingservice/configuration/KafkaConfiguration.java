package com.danijelsudimac.trackingservice.configuration;

import com.danijelsudimac.trackingservice.service.TrackingMetrics;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Optional;
import java.util.Set;

@Profile("!test")
@Configuration
@Slf4j
public class KafkaConfiguration {

    private static final String ERROR_LOG_MESSAGE = "Error processing record with key {}: {} with message {}. Sending to {} topic";
    private static final Set<Class<? extends Throwable>> INVALID_MESSAGE_EXCEPTIONS = Set.of(
            DeserializationException.class,
            SerializationException.class,
            IllegalArgumentException.class
    ); //poison pill exceptions

    public final String topic;
    public final String topicDtl;
    public final String topicInvalidMessages;
    public final TrackingMetrics trackingMetrics;

    public KafkaConfiguration(@Value("${application.kafka.shipment-topic}") String topic,
                              @Value("${application.kafka.shipment-topic-dlt}") String topicDtl,
                              @Value("${application.kafka.topic-invalid-messages}") String topicInvalidMessages,
                              TrackingMetrics trackingMetrics) {
        this.topic = topic;
        this.topicDtl = topicDtl;
        this.topicInvalidMessages = topicInvalidMessages;
        this.trackingMetrics = trackingMetrics;
    }
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        var recoverer =
                new DeadLetterPublishingRecoverer(template,
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

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory, DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        factory.setConcurrency(3);
        factory.setBatchListener(false);
        factory.getContainerProperties().setMissingTopicsFatal(false);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }
}