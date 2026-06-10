package com.danijelsudimac.trackingservice.configuration;

import com.danijelsudimac.notification.service.client.api.NotificationControllerApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public NotificationControllerApi notificationControllerApi() {
        return new NotificationControllerApi();
    }
}
