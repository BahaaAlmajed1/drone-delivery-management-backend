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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AdminServiceTest {

    @Autowired
    AdminService adminService;
    @Autowired
    EndUserService endUserService;
    @Autowired
    EndUserRepository endUserRepository;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    JobRepository jobRepository;
    @Autowired
    DroneRepository droneRepository;
    @Autowired
    DroneService droneService;

    @Test
    void adminCanUpdateOrderBeforePickup() {
        EndUser endUser = endUserRepository.save(new EndUser("unit-admin-user"));
        DeliveryOrder order = endUserService.submitOrder(endUser.getId(), 1.0, 2.0, 3.0, 4.0);

        DeliveryOrder updated = adminService.adminUpdateOrder(order.getId(), 5.0, 6.0, 7.0, 8.0);
        assertThat(updated.getOriginLat()).isEqualTo(5.0);
        assertThat(updated.getDestinationLat()).isEqualTo(7.0);
    }

    @Test
    void adminCannotUpdateAfterPickup() {
        EndUser endUser = endUserRepository.save(new EndUser("unit-admin-blocked"));
        DeliveryOrder order = endUserService.submitOrder(endUser.getId(), 1.0, 2.0, 3.0, 4.0);
        Job job = jobRepository.findById(order.getCurrentJobId()).orElseThrow();

        Drone drone = droneRepository.save(new Drone("unit-admin-drone"));
        droneService.heartbeat(drone.getId(), 1.1, 2.1);
        Job reserved = droneService.reserveJob(drone.getId(), job.getId());
        droneService.pickupJob(drone.getId(), reserved.getId());

        assertThatThrownBy(() -> adminService.adminUpdateOrder(order.getId(), 9.0, 9.0, 9.0, 9.0))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Cannot modify an order after pickup");
    }
}
