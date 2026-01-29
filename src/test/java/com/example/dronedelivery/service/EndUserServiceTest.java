package com.example.dronedelivery.service;

import com.example.dronedelivery.domain.*;
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
class EndUserServiceTest {

    @Autowired
    EndUserService endUserService;
    @Autowired
    EndUserRepository endUserRepository;
    @Autowired
    JobRepository jobRepository;
    @Autowired
    OrderRepository orderRepository;

    @Test
    void submitOrderCreatesOpenJob() {
        EndUser endUser = endUserRepository.save(new EndUser("unit-submit-user"));
        DeliveryOrder order = endUserService.submitOrder(endUser.getId(), 2.0, 3.0, 4.0, 5.0);
        Job job = jobRepository.findById(order.getCurrentJobId()).orElseThrow();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SUBMITTED);
        assertThat(job.getStatus()).isEqualTo(JobStatus.OPEN);
    }

    @Test
    void withdrawFailsForNonOwner() {
        EndUser owner = endUserRepository.save(new EndUser("unit-owner"));
        EndUser other = endUserRepository.save(new EndUser("unit-other"));
        DeliveryOrder order = endUserService.submitOrder(owner.getId(), 1.0, 1.0, 2.0, 2.0);

        assertThatThrownBy(() -> endUserService.withdrawOrder(other.getId(), order.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("withdraw your own orders");
    }
}
