package com.danijelsudimac.trackingservice.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TrackingMetrics {

    private static final String MESSAGE_IN_DLT_COUNTER_NAME = "kafka_message_in_dlt";
    private static final String MESSAGE_IN_DLT_COUNTER_DESCRIPTION = "Added message to dlt or poison pill topic";
    private static final String SHIPMENT_CREATED_COUNTER_NAME = "shipment_created";
    private static final String SHIPMENT_CREATED_COUNTER_DESCRIPTION = "Shipment created in db";
    private static final String SHIPMENT_UPDATED_COUNTER_NAME = "shipment_updated";
    private static final String SHIPMENT_UPDATED_COUNTER_DESCRIPTION = "Shipment created in db";
    private static final String SHIPMENT_DELETED_COUNTER_NAME = "shipment_created";
    private static final String SHIPMENT_DELETED_COUNTER_DESCRIPTION = "Shipment created in db";
    private final Counter kafkaDltMessages;
    private final Counter shipmentCreated;
    private final Counter shipmentUpdated;
    private final Counter shipmentDeleted;

    public TrackingMetrics(MeterRegistry registry) {
        this.kafkaDltMessages = Counter.builder(MESSAGE_IN_DLT_COUNTER_NAME)
                        .description(MESSAGE_IN_DLT_COUNTER_DESCRIPTION)
                        .register(registry);
        this.shipmentCreated = Counter.builder(SHIPMENT_CREATED_COUNTER_NAME)
                        .description(SHIPMENT_CREATED_COUNTER_DESCRIPTION)
                        .register(registry);
        this.shipmentUpdated = Counter.builder(SHIPMENT_UPDATED_COUNTER_NAME)
                .description(SHIPMENT_UPDATED_COUNTER_DESCRIPTION)
                .register(registry);
        this.shipmentDeleted = Counter.builder(SHIPMENT_DELETED_COUNTER_NAME)
                .description(SHIPMENT_DELETED_COUNTER_DESCRIPTION)
                .register(registry);
    }

    public void incrementMessageInDlt() {
        kafkaDltMessages.increment();
    }
    public void incrementShipmentCreated() {
        log.info("INCREMENT shipmentCreated");
        shipmentCreated.increment();
    }
    public void incrementShipmentUpdated() { shipmentUpdated.increment();}
    public void incrementShipmentDeleted() { shipmentDeleted.increment();}
}
