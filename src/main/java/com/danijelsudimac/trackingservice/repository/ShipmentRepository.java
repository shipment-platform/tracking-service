package com.danijelsudimac.trackingservice.repository;

import com.danijelsudimac.trackingservice.model.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByExternalIdAndClientId(String externalId, Long clientId);
}
