package com.example.dronedelivery.service;

import com.example.dronedelivery.domain.*;
import com.example.dronedelivery.repo.DroneRepository;
import com.example.dronedelivery.repo.EndUserRepository;
import com.example.dronedelivery.repo.JobRepository;
import com.example.dronedelivery.repo.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class DroneServiceTest {

    @Autowired
    DroneService droneService;
    @Autowired
    EndUserService endUserService;
    @Autowired
    DroneRepository droneRepository;
    @Autowired
    EndUserRepository endUserRepository;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    JobRepository jobRepository;

    @Test
    void brokenDroneCannotReserveJob() {
        EndUser endUser = endUserRepository.save(new EndUser("unit-broken-user"));
        DeliveryOrder order = endUserService.submitOrder(endUser.getId(), 1.0, 2.0, 3.0, 4.0);
        Job job = jobRepository.findById(order.getCurrentJobId()).orElseThrow();

        Drone drone = droneRepository.save(new Drone("unit-broken-drone"));
        droneService.heartbeat(drone.getId(), 5.0, 6.0);
        drone.setStatus(DroneStatus.BROKEN);
        droneRepository.save(drone);

        assertThatThrownBy(() -> droneService.reserveJob(drone.getId(), job.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Broken drones cannot reserve jobs");
    }

    @Test
    void excludedDroneCannotReserveHandoffJob() {
        EndUser endUser = endUserRepository.save(new EndUser("unit-excluded-user"));
        DeliveryOrder order = endUserService.submitOrder(endUser.getId(), 10.0, 20.0, 30.0, 40.0);

        Drone brokenDrone = droneRepository.save(new Drone("unit-excluded-drone"));
        droneService.heartbeat(brokenDrone.getId(), 10.1, 20.1);

        Job initial = jobRepository.findById(order.getCurrentJobId()).orElseThrow();
        Job reserved = droneService.reserveJob(brokenDrone.getId(), initial.getId());
        droneService.pickupJob(brokenDrone.getId(), reserved.getId());
        droneService.droneMarkBroken(brokenDrone.getId());

        DeliveryOrder updated = orderRepository.findById(order.getId()).orElseThrow();
        Job handoff = jobRepository.findById(updated.getCurrentJobId()).orElseThrow();

        assertThat(handoff.getType()).isEqualTo(JobType.HANDOFF_PICKUP_AND_DELIVER);
        assertThat(handoff.getExcludedDroneId()).isEqualTo(brokenDrone.getId());
        brokenDrone.setStatus(DroneStatus.FIXED);
        assertThatThrownBy(() -> droneService.reserveJob(brokenDrone.getId(), handoff.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("picked up by a different drone");
    }

    @Test
    void currentJobThrowsWhenNoneAssigned() {
        Drone drone = droneRepository.save(new Drone("unit-no-job"));
        droneService.heartbeat(drone.getId(), 7.0, 8.0);
        assertThatThrownBy(() -> droneService.getCurrentJobForDrone(drone.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Drone has no current job");
    }
}
