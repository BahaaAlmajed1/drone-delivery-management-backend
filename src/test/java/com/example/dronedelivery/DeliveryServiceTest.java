package com.example.dronedelivery;

import com.example.dronedelivery.domain.*;
import com.example.dronedelivery.repo.DroneRepository;
import com.example.dronedelivery.repo.EndUserRepository;
import com.example.dronedelivery.repo.JobRepository;
import com.example.dronedelivery.repo.OrderRepository;
import com.example.dronedelivery.service.DeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class DeliveryServiceTest {

    @Autowired DeliveryService service;
    @Autowired EndUserRepository endUserRepository;
    @Autowired DroneRepository droneRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired JobRepository jobRepository;

    @Test
    @Transactional
    void submitCreatesOpenJob() {
        EndUser u = endUserRepository.save(new EndUser("u1"));
        DeliveryOrder o = service.submitOrder(u.getId(), 1.0, 2.0, 3.0, 4.0);

        assertThat(o.getStatus()).isEqualTo(OrderStatus.SUBMITTED);
        assertThat(o.getCurrentJobId()).isNotNull();

        Job j = jobRepository.findById(o.getCurrentJobId()).orElseThrow();
        assertThat(j.getStatus()).isEqualTo(JobStatus.OPEN);
        assertThat(j.getType()).isEqualTo(JobType.PICKUP_AND_DELIVER);
    }

    @Test
    @Transactional
    void breakingInProgressCreatesHandoffJobExcludedFromSameDrone() {
        EndUser u = endUserRepository.save(new EndUser("u2"));
        DeliveryOrder o = service.submitOrder(u.getId(), 10.0, 20.0, 30.0, 40.0);
        Job initial = jobRepository.findById(o.getCurrentJobId()).orElseThrow();

        Drone d = droneRepository.save(new Drone("d1"));
        service.heartbeat(d.getId(), 11.0, 22.0);

        Job reserved = service.reserveJob(d.getId(), initial.getId());
        assertThat(reserved.getStatus()).isEqualTo(JobStatus.RESERVED);

        Job inProgress = service.pickupJob(d.getId(), reserved.getId());
        assertThat(inProgress.getStatus()).isEqualTo(JobStatus.IN_PROGRESS);

        Drone broken = service.droneMarkBroken(d.getId());
        assertThat(broken.getStatus()).isEqualTo(DroneStatus.BROKEN);
        assertThat(broken.getCurrentJobId()).isNull();

        DeliveryOrder updated = orderRepository.findById(o.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.HANDOFF_REQUESTED);

        Job handoff = jobRepository.findById(updated.getCurrentJobId()).orElseThrow();
        assertThat(handoff.getType()).isEqualTo(JobType.HANDOFF_PICKUP_AND_DELIVER);
        assertThat(handoff.getStatus()).isEqualTo(JobStatus.OPEN);
        assertThat(handoff.getExcludedDroneId()).isEqualTo(d.getId());
        assertThat(handoff.getPickupLat()).isEqualTo(11.0);
        assertThat(handoff.getPickupLng()).isEqualTo(22.0);
    }
}
