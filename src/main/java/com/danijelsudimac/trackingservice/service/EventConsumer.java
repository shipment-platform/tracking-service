package com.danijelsudimac.trackingservice.service;

import com.danijelsudimac.shipmentservice.model.event.ShipmentCreatedEvent;
import com.danijelsudimac.shipmentservice.model.event.ShipmentDeletedEvent;
import com.danijelsudimac.shipmentservice.model.event.ShipmentUpdatedEvent;

public interface EventConsumer {
    void consume(ShipmentCreatedEvent event);
    void consume(ShipmentUpdatedEvent event);
    void consume(ShipmentDeletedEvent event);
}
