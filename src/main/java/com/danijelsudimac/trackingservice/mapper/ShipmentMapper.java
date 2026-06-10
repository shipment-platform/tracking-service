package com.danijelsudimac.trackingservice.mapper;

import com.danijelsudimac.shipmentservice.model.event.ShipmentCreatedEvent;
import com.danijelsudimac.trackingservice.model.entity.Shipment;
import com.danijelsudimac.trackingservice.model.ShipmentStatus;
import com.danijelsudimac.trackingservice.model.ShippingMethod;
import org.mapstruct.*;

import java.time.Instant;

@Mapper(componentModel = "spring")
public interface ShipmentMapper {

    @ValueMappings({
            @ValueMapping(source = "SHIPMENT_STATUS_CREATED", target = "CREATED"),
            @ValueMapping(source = "SHIPMENT_STATUS_PENDING", target = "PENDING"),
            @ValueMapping(source = "SHIPMENT_STATUS_SHIPPED", target = "SHIPPED"),
            @ValueMapping(source = "SHIPMENT_STATUS_IN_TRANSIT", target = "IN_TRANSIT"),
            @ValueMapping(source = "SHIPMENT_STATUS_DELIVERED", target = "DELIVERED"),
            @ValueMapping(source = "SHIPMENT_STATUS_CANCELLED", target = "CANCELLED"),
            @ValueMapping(source = "SHIPMENT_STATUS_UNSPECIFIED", target = "CREATED"),
            @ValueMapping(source = "UNRECOGNIZED", target = "CREATED")
    })
    ShipmentStatus map(com.danijelsudimac.shipmentservice.model.common.ShipmentStatus status);

    @ValueMappings({
            @ValueMapping(source = "SHIPPING_METHOD_STANDARD", target = "STANDARD"),
            @ValueMapping(source = "SHIPPING_METHOD_EXPRESS", target = "EXPRESS"),
            @ValueMapping(source = "SHIPPING_METHOD_OVERNIGHT", target = "OVERNIGHT"),
            @ValueMapping(source = "SHIPPING_METHOD_UNSPECIFIED", target = "STANDARD"),
            @ValueMapping(source = "UNRECOGNIZED", target = "STANDARD")
    })
    ShippingMethod map(com.danijelsudimac.shipmentservice.model.common.ShippingMethod method);

    default Instant mapInstant(long value) {
        return value == 0
                ? null
                : Instant.ofEpochMilli(value);
    }

    @Mapping(source = "shipmentCreatedEvent.eventTimestamp", target = "lastEventTimestamp")
    @Mapping(source = "shipmentCreatedEvent.itemsList", target = "items")
    Shipment toShipment(ShipmentCreatedEvent shipmentCreatedEvent);
}
