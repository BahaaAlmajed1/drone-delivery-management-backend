package com.example.dronedelivery.service;

import com.example.dronedelivery.api.dto.OrderDtos;
import com.example.dronedelivery.domain.*;
import com.example.dronedelivery.repo.DroneRepository;
import com.example.dronedelivery.repo.JobRepository;
import com.example.dronedelivery.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final DroneRepository droneRepository;
    private final OrderRepository orderRepository;
    private final JobRepository jobRepository;
    private final DeliveryServiceHelper helper;

    @Transactional(readOnly = true)
    public List<DeliveryOrder> listAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public DeliveryOrder adminUpdateOrder(UUID orderId, Double originLat, Double originLng, Double destLat, Double destLng) {
        DeliveryOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.FAILED || order.getStatus() == OrderStatus.CANCELED) {
            throw ApiException.badRequest("Cannot modify an order in terminal state: " + order.getStatus());
        }

        Job currentJob = helper.requireCurrentJob(order);

        // Keep semantics consistent with "withdraw before pickup": after pickup, origin/destination changes are risky.
        if (currentJob.getStatus() == JobStatus.IN_PROGRESS) {
            throw ApiException.badRequest("Cannot modify an order after pickup (job already in progress).");
        }

        // Destination can be updated for any non-terminal order before pickup.
        if (destLat != null && destLng != null) {
            order.setDestination(destLat, destLng);
            currentJob.setDropoff(destLat, destLng);
        }

        // Origin update is meaningful only for the initial leg (pickup at origin).
        if (originLat != null && originLng != null) {
            if (currentJob.getType() != JobType.PICKUP_AND_DELIVER) {
                throw ApiException.badRequest("Cannot change origin during handoff flow (pickup is at broken drone location).");
            }
            order.setOrigin(originLat, originLng);
            currentJob.setPickup(originLat, originLng);
        }

        jobRepository.save(currentJob);
        orderRepository.save(order);
        return order;
    }

    @Transactional(readOnly = true)
    public List<Drone> listDrones() {
        return droneRepository.findAll();
    }

    @Transactional
    public Drone adminSetDroneStatus(UUID droneId, DroneStatus status) {
        Drone d = droneRepository.findById(droneId)
                .orElseThrow(() -> ApiException.notFound("Drone not found: " + droneId));
        if (status == DroneStatus.BROKEN) {
            // Apply the same broken behavior as if the drone reported itself broken.
            helper.markDroneBrokenInternal(d);
        } else {
            d.setStatus(status);
            droneRepository.save(d);
        }
        return d;
    }

    @Transactional(readOnly = true)
    public OrderDtos.Progress computeProgress(DeliveryOrder order) {
        return helper.computeProgress(order);
    }
}
