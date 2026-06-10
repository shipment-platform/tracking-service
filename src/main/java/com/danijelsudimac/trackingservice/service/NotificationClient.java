package com.danijelsudimac.trackingservice.service;

import com.danijelsudimac.notification.service.client.api.NotificationControllerApi;
import com.danijelsudimac.notification.service.client.model.ShipmentNotificationRequest;
import com.danijelsudimac.trackingservice.model.entity.Shipment;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationClient {

    private final NotificationControllerApi notificationControllerApi;

    @Retry(name = "notification-service")
    @CircuitBreaker(
            name = "notification-service"
    )
    public void sendShipmentCreated(Shipment shipment) {
        var request =  new ShipmentNotificationRequest();
        request.setEmail(shipment.getRecipientEmail());
        request.setExternalId(shipment.getExternalId());
        request.setTrackingNumber(shipment.getTrackingNumber());
        log.info("Calling notification service for shipment with external id: {}", shipment.getExternalId());
        notificationControllerApi.notifyShipmentCreated(request);
    }

    public void sendShipmentUpdated(Shipment shipment) {
        //TODO
    }

    public void sendShipmentDeleted(Shipment shipment) {
        //TODO
    }
}
