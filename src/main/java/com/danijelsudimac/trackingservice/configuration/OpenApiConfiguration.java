package com.danijelsudimac.trackingservice.configuration;

import com.danijelsudimac.notification.service.client.ApiClient;
import com.danijelsudimac.notification.service.client.api.NotificationControllerApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public NotificationControllerApi notificationControllerApi(@Value("${application.notification-service.base-path}") String basePath) {
        var apiClient = new ApiClient();
        apiClient.setBasePath("basePath");
        return new NotificationControllerApi(apiClient);
    }
}
