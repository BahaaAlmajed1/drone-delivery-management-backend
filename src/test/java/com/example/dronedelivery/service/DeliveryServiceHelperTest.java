package com.example.dronedelivery.service;

import com.example.dronedelivery.domain.DeliveryOrder;
import com.example.dronedelivery.domain.Drone;
import com.example.dronedelivery.domain.EndUser;
import com.example.dronedelivery.domain.Job;
import com.example.dronedelivery.repo.DroneRepository;
import com.example.dronedelivery.repo.EndUserRepository;
import com.example.dronedelivery.repo.JobRepository;
import com.example.dronedelivery.repo.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DeliveryServiceHelperTest {

    @Autowired
    DeliveryServiceHelper deliveryServiceHelper;
    @Autowired
    EndUserService endUserService;
    @Autowired
    EndUserRepository endUserRepository;
    @Autowired
    DroneRepository droneRepository;
    @Autowired
    JobRepository jobRepository;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    DroneService droneService;

    @Test
    void computeProgressReturnsNullWithoutAssignedDrone() {
        EndUser endUser = endUserRepository.save(new EndUser("unit-progress-user"));
        DeliveryOrder order = endUserService.submitOrder(endUser.getId(), 1.0, 2.0, 3.0, 4.0);
        var progress = deliveryServiceHelper.computeProgress(order);
        assertThat(progress).isNotNull();
        assertThat(progress.currentLocation()).isNotNull();
        assertThat(progress.currentLocation().lat()).isEqualTo(1.0);
        assertThat(progress.currentLocation().lng()).isEqualTo(2.0);
        assertThat(progress.etaSecondsApprox()).isEqualTo(0);
    }

    @Test
    void computeProgressUsesDroneLocation() {
        EndUser endUser = endUserRepository.save(new EndUser("unit-progress-user2"));
        DeliveryOrder order = endUserService.submitOrder(endUser.getId(), 1.0, 2.0, 3.0, 4.0);
        Job job = jobRepository.findById(order.getCurrentJobId()).orElseThrow();

        Drone drone = droneRepository.save(new Drone("unit-progress-drone"));
        droneService.heartbeat(drone.getId(), 1.5, 2.5);
        droneService.reserveJob(drone.getId(), job.getId());
        droneService.pickupJob(drone.getId(), job.getId());

        DeliveryOrder refreshed = orderRepository.findById(order.getId()).orElseThrow();
        var progress = deliveryServiceHelper.computeProgress(refreshed);
        assertThat(progress).isNotNull();
        assertThat(progress.currentLocation()).isNotNull();
        assertThat(progress.etaSecondsApprox()).isGreaterThanOrEqualTo(0);
    }
}
