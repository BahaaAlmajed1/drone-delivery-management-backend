package com.example.dronedelivery.service;

import com.example.dronedelivery.domain.*;
import com.example.dronedelivery.repo.DroneRepository;
import com.example.dronedelivery.repo.EndUserRepository;
import com.example.dronedelivery.repo.JobRepository;
import com.example.dronedelivery.repo.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JobAssignmentSchedulerTest {

    @Autowired
    JobAssignmentScheduler scheduler;
    @Autowired
    DroneService droneService;
    @Autowired
    EndUserService endUserService;
    @Autowired
    DroneRepository droneRepository;
    @Autowired
    EndUserRepository endUserRepository;
    @Autowired
    JobRepository jobRepository;
    @Autowired
    OrderRepository orderRepository;

    @BeforeEach
    void resetData() {
        jobRepository.deleteAll();
        orderRepository.deleteAll();
        droneRepository.deleteAll();
        endUserRepository.deleteAll();
    }

    @Test
    void assignsClosestDroneToOpenJob() {
        EndUser endUser = endUserRepository.save(new EndUser("scheduler-unit-user"));
        DeliveryOrder order = endUserService.submitOrder(endUser.getId(), 10.0, 10.0, 20.0, 20.0);

        Drone nearDrone = droneRepository.save(new Drone("scheduler-near-drone"));
        Drone farDrone = droneRepository.save(new Drone("scheduler-far-drone"));

        droneService.heartbeat(nearDrone.getId(), 10.01, 10.01);
        droneService.heartbeat(farDrone.getId(), 30.0, 30.0);

        scheduler.assignJobsToClosestDrones();

        Job job = jobRepository.findById(order.getCurrentJobId()).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(JobStatus.RESERVED);
        assertThat(job.getAssignedDroneId()).isEqualTo(nearDrone.getId());

        Drone updatedNear = droneRepository.findById(nearDrone.getId()).orElseThrow();
        Drone updatedFar = droneRepository.findById(farDrone.getId()).orElseThrow();
        assertThat(updatedNear.getCurrentJobId()).isEqualTo(job.getId());
        assertThat(updatedFar.getCurrentJobId()).isNull();
    }

    @Test
    void marksStaleDroneAsBroken() {
        Drone drone = droneRepository.save(new Drone("scheduler-stale-drone"));
        drone.setStatus(DroneStatus.ACTIVE);
        drone.setLastHeartbeatAt(Instant.now().minus(Duration.ofMinutes(10)));
        droneRepository.save(drone);

        scheduler.markStaleDronesBroken();

        Drone refreshed = droneRepository.findById(drone.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(DroneStatus.BROKEN);
    }
}
