package com.danijelsudimac.trackingservice.model.entity;

import com.danijelsudimac.trackingservice.model.ShipmentStatus;
import com.danijelsudimac.trackingservice.model.ShippingMethod;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "shipment",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_client_external",
                        columnNames = {
                                "client_id",
                                "external_id"
                        }
                )
        }
)
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long clientId;

    @NotBlank
    private String idempotencyKey;

    @NotBlank
    @Size(max = 50)
    private String externalId;

    @NotBlank
    @Size(max = 50)
    private String trackingNumber;

    @NotBlank
    @Size(max = 50)
    private String orderId;

    @Nullable
    @Enumerated(EnumType.STRING)
    @Column(name = "shipment_status")
    private ShipmentStatus status;

    @NotBlank
    @Size(max = 50)
    private String carrier;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_method")
    private ShippingMethod shippingMethod;

    @NotBlank
    @Size(max = 100)
    private String recipientName;

    @NotNull
    @OneToOne(
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JoinColumn(
            name = "recipient_address_id",
            unique = true,
            nullable = false
    )
    private Address recipientAddress;

    @NotBlank
    @Pattern(regexp = "\\+?[0-9\\-]+")
    private String recipientPhoneNumber;

    @Email
    @NotBlank
    private String recipientEmail;

    @NotBlank
    @Size(max = 100)
    private String originName;

    @NotNull
    @OneToOne(
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JoinColumn(
            name = "origin_address_id",
            unique = true,
            nullable = false
    )
    private Address originAddress;

    @NotBlank
    @Pattern(regexp = "\\+?[0-9\\-]+")
    private String originPhoneNumber;

    @Email
    @NotBlank
    private String originEmail;

    @NotNull
    @OneToMany(
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JoinColumn(
            name = "shipment_id",
            nullable = false
    )
    private List<Item> items = new ArrayList<>();

    @Nullable
    private Instant estimatedPickup;

    @Nullable
    private Instant estimatedDelivery;

    @CreatedDate
    private Instant createdAt;

    @Nullable
    private Instant actualPickup;

    @Nullable
    private Instant actualDelivery;

    @LastModifiedDate
    private Instant updatedAt;

    @Nullable
    private Instant lastEventTimestamp;
}