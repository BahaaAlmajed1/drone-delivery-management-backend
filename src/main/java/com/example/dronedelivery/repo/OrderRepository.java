package com.example.dronedelivery.repo;

import com.example.dronedelivery.domain.DeliveryOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<DeliveryOrder, UUID> {
    List<DeliveryOrder> findByCreatedByEndUserIdOrderByCreatedAtDesc(UUID endUserId);
}
