package com.danijelsudimac.trackingservice.service;

import com.danijelsudimac.shipmentservice.model.event.ShipmentCreatedEvent;
import com.danijelsudimac.shipmentservice.model.event.ShipmentDeletedEvent;
import com.danijelsudimac.shipmentservice.model.event.ShipmentUpdatedEvent;
import com.danijelsudimac.shipmentservice.model.common.ShipmentStatus;
import com.danijelsudimac.trackingservice.mapper.ShipmentMapper;
import com.danijelsudimac.trackingservice.model.entity.Shipment;
import com.danijelsudimac.trackingservice.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ShipmentMapper shipmentMapper;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private TrackingMetrics trackingMetrics;

    @InjectMocks
    private ShipmentService shipmentService;

    @Test
    void shouldCreateShipment() {
        var event = ShipmentCreatedEvent.getDefaultInstance();
        var shipment = new Shipment();
        when(shipmentMapper.toShipment(event)).thenReturn(shipment);
        shipmentService.process(event);

        verify(shipmentMapper).toShipment(event);
        verify(shipmentRepository).save(shipment);
        verify(notificationClient).sendShipmentCreated(shipment);
        verify(trackingMetrics).incrementShipmentCreated();
    }

    @Test
    void shouldDoNothingWhenShipmentNotFound() {
        var event = ShipmentUpdatedEvent.getDefaultInstance();
        when(shipmentRepository.findByExternalIdAndClientId(event.getExternalId(), event.getClientId()))
                .thenReturn(Optional.empty());

        shipmentService.process(event);

        verify(shipmentRepository, never()).save(any());
        verify(notificationClient, never()).sendShipmentUpdated(any());
        verify(trackingMetrics, never()).incrementShipmentUpdated();
    }

    @Test
    void shouldIgnoreOutdatedEvent() {
        var shipment = new Shipment();
        shipment.setLastEventTimestamp(Instant.now());

        var event = ShipmentUpdatedEvent.newBuilder()
                .setEventTimestamp(Instant.now().minusSeconds(60).toEpochMilli())
                .build();

        when(shipmentRepository.findByExternalIdAndClientId(any(), any())).thenReturn(Optional.of(shipment));
        shipmentService.process(event);
        verify(shipmentRepository, never()).save(any());
        verify(notificationClient, never()).sendShipmentUpdated(any());
    }

    @Test
    void shouldNotOverrideTrackingNumberWhenMismatchOccurs() {
        var shipment = new Shipment();
        shipment.setTrackingNumber("OLD");

        var event = ShipmentUpdatedEvent.newBuilder().setTrackingNumber("NEW").build();
        when(shipmentRepository.findByExternalIdAndClientId(any(), any())).thenReturn(Optional.of(shipment));

        shipmentService.process(event);
        assertThat(shipment.getTrackingNumber()).isEqualTo("OLD");
        verify(shipmentRepository).save(shipment);
    }

    @Test
    void shouldNotOverwriteFieldsWhenEventHasZeroOrNullValues() {
        // given
        var event = ShipmentUpdatedEvent.newBuilder()
                .setEventTimestamp(System.currentTimeMillis())
                .setActualPickup(0L)
                .setActualDelivery(0L)
                .setEstimatedPickup(0L)
                .setEstimatedDelivery(0L)
                .setStatus(ShipmentStatus.SHIPMENT_STATUS_CREATED)
                .build();

        Shipment shipment = new Shipment();
        shipment.setLastEventTimestamp(Instant.now().minusSeconds(60));

        Instant originalPickup = shipment.getActualPickup();
        Instant originalDelivery = shipment.getActualDelivery();

        when(shipmentRepository.findByExternalIdAndClientId(event.getExternalId(), event.getClientId()))
                .thenReturn(Optional.of(shipment));

        // when
        shipmentService.process(event);

        // then
        verify(shipmentRepository).save(shipment);

        // existing values must remain unchanged
        assertThat(shipment.getActualPickup()).isEqualTo(originalPickup);
        assertThat(shipment.getActualDelivery()).isEqualTo(originalDelivery);
        verify(notificationClient).sendShipmentUpdated(shipment);
        verify(trackingMetrics).incrementShipmentUpdated();
    }

    @Test
    void shouldNotFailWhenNotificationFails() {
        var event = ShipmentCreatedEvent.getDefaultInstance();
        var shipment = new Shipment();

        when(shipmentMapper.toShipment(event)).thenReturn(shipment);

        doThrow(new RuntimeException("boom")).when(notificationClient).sendShipmentCreated(shipment);

        assertThatCode(() -> shipmentService.process(event)).doesNotThrowAnyException();

        verify(shipmentRepository).save(shipment);
        verify(trackingMetrics)
                .incrementShipmentCreated();
    }

    @Test
    void shouldUpdateShipmentWhenValidEvent() {
        // given
        var event = ShipmentUpdatedEvent.newBuilder().setEventTimestamp(System.currentTimeMillis()).build();

        Shipment shipment = new Shipment();
        shipment.setLastEventTimestamp(Instant.now().minusSeconds(60));

        when(shipmentRepository.findByExternalIdAndClientId(event.getExternalId(), event.getClientId()))
                .thenReturn(Optional.of(shipment));

        // when
        shipmentService.process(event);

        // then
        verify(shipmentRepository).save(shipment);
        verify(notificationClient).sendShipmentUpdated(shipment);
        verify(trackingMetrics).incrementShipmentUpdated();
    }


    @Test
    void shouldDeleteShipmentWhenExists() {
        // given
        var event = ShipmentDeletedEvent.newBuilder()
                .setExternalId("ext-1")
                .setClientId(1L)
                .build();

        Shipment shipment = new Shipment();

        when(shipmentRepository.findByExternalIdAndClientId(event.getExternalId(), event.getClientId()))
                .thenReturn(Optional.of(shipment));

        // when
        shipmentService.process(event);

        // then
        verify(shipmentRepository).delete(shipment);
        verify(notificationClient).sendShipmentDeleted(shipment);
        verify(trackingMetrics).incrementShipmentDeleted();
    }
}
