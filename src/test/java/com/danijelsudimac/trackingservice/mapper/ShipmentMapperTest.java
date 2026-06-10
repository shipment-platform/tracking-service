package com.danijelsudimac.trackingservice.mapper;

import com.danijelsudimac.trackingservice.model.entity.Shipment;
import com.danijelsudimac.shipmentservice.model.event.ShipmentCreatedEvent;
import com.danijelsudimac.shipmentservice.model.common.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
public class ShipmentMapperTest {

    private final ShipmentMapper mapper = Mappers.getMapper(ShipmentMapper.class);

    @Test
    void shouldMapShipmentCreatedEventToShipment() {
        Item item1 = Item.newBuilder()
                .setName("Laptop")
                .setQuantity(1)
                .setUnit("pcs")
                .setWeight(2.3)
                .build();
        Item item2 = Item.newBuilder()
                .setName("Mouse")
                .setQuantity(2)
                .setUnit("pcs")
                .setWeight(0.2)
                .build();
        Instant now = Instant.now();
        ShipmentCreatedEvent event = ShipmentCreatedEvent.newBuilder()
                .setIdempotencyKey("idem-1")
                .setCarrier("DHL")
                .setClientId(123L)

                .setEstimatedDelivery(now.plusSeconds(3600).toEpochMilli())
                .setEstimatedPickup(now.toEpochMilli())
                .setEventTimestamp(now.toEpochMilli())

                .setExternalId("EXT-1")

                .addAllItems(List.of(item1,item2)) // ili lista Item objekata

                .setOrderId("ORDER-1")

                .setOriginAddress(Address.newBuilder()
                        .setAddressLine("Street 1")
                        .setCity("Belgrade")
                        .setCountry("RS")
                        .setPostalCode("11000")
                        .setState("Central Serbia")
                        .build())

                .setOriginEmail("origin@mail.com")
                .setOriginName("Origin Name")
                .setOriginPhoneNumber("+38160111222")

                .setRecipientAddress(Address.newBuilder()
                        .setAddressLine("Street 2")
                        .setCity("Novi Sad")
                        .setCountry("RS")
                        .setPostalCode("21000")
                        .setState("Vojvodina")
                        .build())

                .setRecipientEmail("recipient@mail.com")
                .setRecipientName("Recipient Name")
                .setRecipientPhoneNumber("+38160111233")

                .setShippingMethod(ShippingMethod.SHIPPING_METHOD_EXPRESS)
                .setStatus(ShipmentStatus.SHIPMENT_STATUS_CREATED)

                .setTrackingNumber("TRACK-1")
                .build();

        Shipment shipment = mapper.toShipment(event);

        assertNotNull(shipment);

        assertEquals(event.getClientId(), shipment.getClientId());
        assertEquals(event.getIdempotencyKey(), shipment.getIdempotencyKey());
        assertEquals(event.getExternalId(), shipment.getExternalId());
        assertEquals(event.getTrackingNumber(), shipment.getTrackingNumber());
        assertEquals(event.getOrderId(), shipment.getOrderId());
        assertEquals(event.getCarrier(), shipment.getCarrier());

        assertEquals(event.getEstimatedPickup(), shipment.getEstimatedPickup().toEpochMilli());
        assertEquals(event.getEstimatedDelivery(), shipment.getEstimatedDelivery().toEpochMilli());

        assertEquals(event.getStatus().name(), "SHIPMENT_STATUS_" + shipment.getStatus().name());
        assertEquals(event.getShippingMethod().name(), "SHIPPING_METHOD_" + shipment.getShippingMethod().name());

        assertEquals(event.getOriginEmail(), shipment.getOriginEmail());
        assertEquals(event.getRecipientEmail(), shipment.getRecipientEmail());

        assertEquals(event.getEventTimestamp(), shipment.getLastEventTimestamp().toEpochMilli());
        assertTrue(itemsMatch(event.getItemsList(), shipment.getItems()));
        assertTrue(addressMatch(event.getOriginAddress(), shipment.getOriginAddress()));
        assertTrue(addressMatch(event.getRecipientAddress(), shipment.getRecipientAddress()));
    }

    private boolean addressMatch(Address firstAddress, com.danijelsudimac.trackingservice.model.entity.Address secondAddress) {
        if (Objects.equals(firstAddress.getState(), secondAddress.getState()) &&
                Objects.equals(firstAddress.getAddressLine(), secondAddress.getAddressLine()) &&
                Objects.equals(firstAddress.getCity(), secondAddress.getCity()) &&
                Objects.equals(firstAddress.getCountry(), secondAddress.getCountry()) &&
                Objects.equals(firstAddress.getPostalCode(), secondAddress.getPostalCode())) {
            return true;
        } else {
            return false;
        }
    }

    private boolean itemsMatch(List<Item> itemsList, List<com.danijelsudimac.trackingservice.model.entity.Item> items) {
        if (itemsList.size() != items.size()) {
            return false;
        }
        for (int i = 0; i < itemsList.size(); i++) {
            Item item1 = itemsList.get(i);
            com.danijelsudimac.trackingservice.model.entity.Item item2 = items.get(i);
            if (!item1.getName().equals(item2.getName()) ||
                    item1.getQuantity() != item2.getQuantity() ||
                    !item1.getUnit().equals(item2.getUnit()) ||
                    item1.getWeight() != item2.getWeight()) {
                return false;
            }
        }
        return true;
    }
}
