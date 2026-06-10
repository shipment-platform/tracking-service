package com.danijelsudimac.trackingservice.service;

import com.danijelsudimac.shipmentservice.model.event.ShipmentCreatedEvent;
import com.danijelsudimac.shipmentservice.model.event.ShipmentDeletedEvent;
import com.danijelsudimac.shipmentservice.model.event.ShipmentUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@EnableKafka
@Profile("!test")
@Slf4j
@RequiredArgsConstructor
@KafkaListener(topics = "${application.kafka.shipment-topic}")
public class KafkaEventConsumer implements EventConsumer{

    private final ShipmentService shipmentService;

    @KafkaHandler
    public void consume(ShipmentCreatedEvent event) {
        shipmentService.process(event);
    }

    @KafkaHandler
    public void consume(ShipmentUpdatedEvent event) {
        shipmentService.process(event);
    }

    @KafkaHandler
    public void consume(ShipmentDeletedEvent event) {
        shipmentService.process(event);
    }
}
