package com.danijelsudimac.trackingservice.service;

import com.danijelsudimac.shipmentservice.model.event.ShipmentCreatedEvent;
import com.danijelsudimac.shipmentservice.model.event.ShipmentDeletedEvent;
import com.danijelsudimac.shipmentservice.model.event.ShipmentUpdatedEvent;
import com.danijelsudimac.trackingservice.mapper.ShipmentMapper;
import com.danijelsudimac.trackingservice.model.entity.Shipment;
import com.danijelsudimac.trackingservice.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentService {

    private static final String SHIPMENT_NOT_FOUND_FOR_UPDATE_MESSAGE = "Shipment with externalId {} not found for update";
    private static final String SHIPMENT_NOT_FOUND_FOR_DELETION_MESSAGE = "Shipment with externalId {} not found for deletion";
    private static final String OUTDATED_EVENT_MESSAGE = "Received outdated event for shipment with externalId {}. Event timestamp: {}, Shipment last event timestamp: {}";
    private static final String TRACKING_NUMBER_MISMATCH_MESSAGE = "Mismatch in tracking number for shipment with externalId {}. Existing tracking number: {}, Event tracking number: {}";
    private static final String ORDER_ID_MISMATCH_MESSAGE = "Mismatch in order id for shipment with externalId {}. Existing order id: {}, Event order id: {}";
    private static final String ERROR_PUBLISHING_TO_NOTIFICATION_SERVICE = "Error publishing event to notification service: {}";

    private final ShipmentRepository repository;
    private final ShipmentMapper mapper;
    private final TrackingMetrics trackingMetrics;
    private final NotificationClient notificationClient;

    public void process(ShipmentCreatedEvent event) {
        var shipment = mapper.toShipment(event);
        repository.save(shipment);
        catchExceptions(notificationClient::sendShipmentCreated,shipment);
        trackingMetrics.incrementShipmentCreated();
    }

    public void process(ShipmentUpdatedEvent event) {
        var shipmentOptional = repository.findByExternalIdAndClientId(event.getExternalId(), event.getClientId());
        shipmentOptional
                .ifPresentOrElse(shipment -> {
                    if (updateShipmentFromEvent(shipment, event)) {
                        repository.save(shipment);
                        catchExceptions(notificationClient::sendShipmentUpdated,shipment);
                        trackingMetrics.incrementShipmentUpdated();
                    }}, () -> log.warn(SHIPMENT_NOT_FOUND_FOR_UPDATE_MESSAGE, event.getExternalId()));
    }

    public void process(ShipmentDeletedEvent event) {
        repository.findByExternalIdAndClientId(event.getExternalId(), event.getClientId())
                .ifPresentOrElse(shipment -> {
                            repository.delete(shipment);
                            catchExceptions(notificationClient::sendShipmentDeleted,shipment);
                            trackingMetrics.incrementShipmentDeleted();
                            },
                        () -> log.warn(SHIPMENT_NOT_FOUND_FOR_DELETION_MESSAGE, event.getExternalId()));
    }

    private boolean updateShipmentFromEvent(Shipment shipment, ShipmentUpdatedEvent event) {

        //check if earlier than shipment lastEventTimestamp, if so ignore
        if (shipment.getLastEventTimestamp() != null && shipment.getLastEventTimestamp().isAfter(Instant.ofEpochMilli(event.getEventTimestamp()))) {
            log.warn(OUTDATED_EVENT_MESSAGE, event.getExternalId(), Instant.ofEpochMilli(event.getEventTimestamp()),
                    shipment.getLastEventTimestamp());
            return false;
        }
        shipment.setLastEventTimestamp(Instant.ofEpochMilli(event.getEventTimestamp()));

        //check if match if exists
        //if not update
        if (shipment.getTrackingNumber() != null) {
            if(!shipment.getTrackingNumber().equals(event.getTrackingNumber())) {
                log.warn(TRACKING_NUMBER_MISMATCH_MESSAGE, event.getExternalId(), shipment.getTrackingNumber(),
                        event.getTrackingNumber());
            }
        } else {
            shipment.setTrackingNumber(event.getTrackingNumber());
        }

        if (shipment.getOrderId() != null) {
            if(!shipment.getOrderId().equals(event.getOrderId())) {
                log.warn(ORDER_ID_MISMATCH_MESSAGE, event.getExternalId(), shipment.getTrackingNumber(),
                        event.getTrackingNumber());
            }
        } else {
            shipment.setOrderId(event.getOrderId());
        }

        if (event.getActualPickup() != 0L) {
            shipment.setActualPickup(Instant.ofEpochMilli(event.getActualPickup()));
        }

        if (event.getActualDelivery() != 0L) {
            shipment.setActualDelivery(Instant.ofEpochMilli(event.getActualDelivery()));
        }

        if (event.getEstimatedPickup() != 0L) {
            shipment.setEstimatedPickup(Instant.ofEpochMilli(event.getEstimatedPickup()));
        }

        if (event.getEstimatedDelivery() != 0L) {
            shipment.setEstimatedDelivery(Instant.ofEpochMilli(event.getEstimatedDelivery()));
        }

        if (event.getStatus() != null) {
            shipment.setStatus(mapper.map(event.getStatus()));
        }

        return true;
    }

    private void catchExceptions(Consumer<Shipment> eventProcessor, Shipment event) {
        try {
            eventProcessor.accept(event);
        } catch (Exception e) {
            log.error(ERROR_PUBLISHING_TO_NOTIFICATION_SERVICE, e.getMessage(), e);
        }
    }
}
