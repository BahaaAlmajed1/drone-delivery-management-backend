package com.example.dronedelivery.service;

import com.example.dronedelivery.api.dto.OrderDtos;
import com.example.dronedelivery.domain.*;
import com.example.dronedelivery.repo.JobRepository;
import com.example.dronedelivery.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EndUserService {

    private final OrderRepository orderRepository;
    private final JobRepository jobRepository;
    private final DeliveryServiceHelper helper;

    @Transactional
    public DeliveryOrder submitOrder(UUID endUserId, double originLat, double originLng, double destLat, double destLng) {
        DeliveryOrder order = new DeliveryOrder(endUserId, originLat, originLng, destLat, destLng);
        order = orderRepository.save(order);

        Job job = new Job(order.getId(), JobType.PICKUP_AND_DELIVER, originLat, originLng, destLat, destLng, null);
        job = jobRepository.save(job);

        order.setCurrentJobId(job.getId());
        orderRepository.save(order);

        return order;
    }

    @Transactional
    public DeliveryOrder withdrawOrder(UUID endUserId, UUID orderId) {
        DeliveryOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found: " + orderId));

        if (!order.getCreatedByEndUserId().equals(endUserId)) {
            throw ApiException.forbidden("You can only withdraw your own orders.");
        }
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.FAILED) {
            throw ApiException.badRequest("Order is already finished and cannot be withdrawn.");
        }

        Job currentJob = helper.requireCurrentJob(order);

        // "Withdraw orders that have not yet been picked up."
        // We interpret "picked up" as job IN_PROGRESS.
        if (currentJob.getStatus() == JobStatus.IN_PROGRESS) {
            throw ApiException.badRequest("Order has been picked up; cannot withdraw.");
        }

        order.setStatus(OrderStatus.CANCELED);
        currentJob.setStatus(JobStatus.CANCELED);
        jobRepository.save(currentJob);
        orderRepository.save(order);
        return order;
    }

    @Transactional(readOnly = true)
    public List<DeliveryOrder> listOrdersForEndUser(UUID endUserId) {
        return orderRepository.findByCreatedByEndUserIdOrderByCreatedAtDesc(endUserId);
    }

    @Transactional(readOnly = true)
    public DeliveryOrder getOrderForEndUser(UUID endUserId, UUID orderId) {
        DeliveryOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found: " + orderId));
        if (!order.getCreatedByEndUserId().equals(endUserId)) {
            throw ApiException.forbidden("You can only view your own orders.");
        }
        return order;
    }

    @Transactional(readOnly = true)
    public OrderDtos.Progress computeProgress(DeliveryOrder order) {
        return helper.computeProgress(order);
    }
}
