package com.danijelsudimac.trackingservice.service;

import com.danijelsudimac.notification.service.client.api.NotificationControllerApi;
import com.danijelsudimac.notification.service.client.model.ShipmentNotificationRequest;
import com.danijelsudimac.trackingservice.model.entity.Shipment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationClientTest {

    @Mock
    private NotificationControllerApi notificationControllerApi;

    @InjectMocks
    private NotificationClient notificationClient;

    @Test
    void shouldSendShipmentCreatedRequest() {
        var shipment = new Shipment();
        shipment.setExternalId("ext-1");
        shipment.setTrackingNumber("TRK-1");
        shipment.setRecipientEmail("test@mail.com");

        notificationClient.sendShipmentCreated(shipment);

        ArgumentCaptor<ShipmentNotificationRequest> captor =
                ArgumentCaptor.forClass(ShipmentNotificationRequest.class);

        verify(notificationControllerApi)
                .notifyShipmentCreated(captor.capture());

        ShipmentNotificationRequest request = captor.getValue();

        assertThat(request.getEmail()).isEqualTo("test@mail.com");
        assertThat(request.getExternalId()).isEqualTo("ext-1");
        assertThat(request.getTrackingNumber()).isEqualTo("TRK-1");
    }
}
