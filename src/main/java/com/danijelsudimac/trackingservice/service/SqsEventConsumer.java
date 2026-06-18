package com.danijelsudimac.trackingservice.service;

import com.danijelsudimac.shipmentservice.model.event.ShipmentCreatedEvent;
import com.danijelsudimac.shipmentservice.model.event.ShipmentDeletedEvent;
import com.danijelsudimac.shipmentservice.model.event.ShipmentUpdatedEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
        value = "app.sqs-consumer.enabled",
        havingValue = "true"
)
public class SqsEventConsumer {

    private static final String MESSAGE_TYPE_HEADER = "message-type";
    private static final String ERROR_ON_DESERIALIZATION_MESSAGE= "Error deserializing message.";
    private final ShipmentService shipmentService;

    @SqsListener("${aws.sqs.shipments-queue-name:shipments.fifo}")
    public void consume(Message<String> message) {
        String eventType = (String) message.getHeaders().get(MESSAGE_TYPE_HEADER);
        try {
            switch (EventType.valueOf(eventType)) {
                case EventType.CREATE_SHIPMENT -> {
                    ShipmentCreatedEvent.Builder builder = ShipmentCreatedEvent.newBuilder();
                    JsonFormat.parser().merge(message.getPayload(), builder);
                    shipmentService.process(builder.build());
                }
                case UPDATE_SHIPMENT -> {
                    ShipmentUpdatedEvent.Builder builder = ShipmentUpdatedEvent.newBuilder();
                    JsonFormat.parser().merge(message.getPayload(), builder);
                    shipmentService.process(builder.build());
                }
                case EventType.DELETE_SHIPMENT -> {
                    ShipmentDeletedEvent.Builder builder = ShipmentDeletedEvent.newBuilder();
                    JsonFormat.parser().merge(message.getPayload(), builder);
                    shipmentService.process(builder.build());
                }
            }
        } catch (InvalidProtocolBufferException e) {
            log.error(ERROR_ON_DESERIALIZATION_MESSAGE, e);
            throw new RuntimeException(e);
        }
    }

    public static enum EventType {
        CREATE_SHIPMENT, UPDATE_SHIPMENT, DELETE_SHIPMENT
    }
}
