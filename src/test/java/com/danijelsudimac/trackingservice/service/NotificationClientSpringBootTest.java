package com.danijelsudimac.trackingservice.service;

import com.danijelsudimac.notification.service.client.api.NotificationControllerApi;
import com.danijelsudimac.trackingservice.configuration.OpenApiConfiguration;
import com.danijelsudimac.trackingservice.model.entity.Shipment;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        NotificationClient.class,
        OpenApiConfiguration.class
})
@ImportAutoConfiguration({
        AopAutoConfiguration.class,
        ConfigurationPropertiesAutoConfiguration.class,
        RetryAutoConfiguration.class,
        CircuitBreakerAutoConfiguration.class
})
@TestPropertySource(properties = {
        "resilience4j.circuitbreaker.instances.notification-service.sliding-window-type=COUNT_BASED",
        "resilience4j.circuitbreaker.instances.notification-service.sliding-window-size=5",
        "resilience4j.circuitbreaker.instances.notification-service.minimum-number-of-calls=5",
        "resilience4j.circuitbreaker.instances.notification-service.failure-rate-threshold=50"
})
class NotificationClientSpringBootTest {

    @MockitoBean
    private NotificationControllerApi notificationControllerApi;

    @MockitoSpyBean
    private NotificationClient notificationClient;

    @Autowired
    private CircuitBreakerRegistry registry;

    @AfterEach
    void tearDown() {
        registry.getAllCircuitBreakers().forEach(cb -> {cb.reset();});
    }

    @Test
    void shouldRetryThreeTimes() {
        Shipment shipment = new Shipment();
        shipment.setExternalId("ext-1");

        doThrow(new RuntimeException("not available")).when(notificationControllerApi)
                .notifyShipmentCreated(any());

        assertThatThrownBy(() -> notificationClient.sendShipmentCreated(shipment)).isInstanceOf(RuntimeException.class);
        verify(notificationClient, times(3)).sendShipmentCreated(shipment);
    }
    @Test
    void shouldOpenCircuitBreakerAfterFailures() {
        doThrow(new RuntimeException("fail")).when(notificationControllerApi).notifyShipmentCreated(any());

        CircuitBreaker cb = registry.circuitBreaker("notification-service");
        var shipment = new Shipment();
        for (int i = 0; i < 2; i++) {
            try {
                notificationClient.sendShipmentCreated(shipment);
            } catch (Exception ignored) {}
        }
        verify(notificationClient, times(5)).sendShipmentCreated(shipment);
        verify(notificationControllerApi, times(5)).notifyShipmentCreated(any());

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
