package com.example.dronedelivery.api;

import com.example.dronedelivery.api.dto.DroneDtos;
import com.example.dronedelivery.api.dto.OrderDtos;
import com.example.dronedelivery.domain.JobStatus;
import com.example.dronedelivery.domain.JobType;
import com.example.dronedelivery.security.AuthRole;
import com.example.dronedelivery.service.JobAssignmentScheduler;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DroneApiTest extends ApiTestSupport {

    @LocalServerPort
    int port;

    @Autowired
    JobAssignmentScheduler scheduler;

    @Test
    void droneCanReservePickupAndCompleteJob() {
        String endUserToken = tokenFor("drone-flow-user", AuthRole.ENDUSER);
        String droneToken = tokenFor("drone-flow-drone", AuthRole.DRONE);

        OrderDtos.OrderResponse order = submitOrder(endUserToken, 5.0, 6.0, 7.0, 8.0);
        heartbeat(droneToken, 5.1, 6.1);

        List<DroneDtos.JobResponse> openJobs = listOpenJobs(droneToken);
        Assertions.assertThat(openJobs).extracting(DroneDtos.JobResponse::id).contains(order.currentJobId());

        DroneDtos.JobResponse reserved = reserveJob(droneToken, order.currentJobId());
        Assertions.assertThat(reserved.status()).isEqualTo(JobStatus.RESERVED);

        DroneDtos.JobResponse inProgress = pickupJob(droneToken, order.currentJobId());
        Assertions.assertThat(inProgress.status()).isEqualTo(JobStatus.IN_PROGRESS);

        DroneDtos.JobResponse completed = completeJob(droneToken, order.currentJobId());
        Assertions.assertThat(completed.status()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void droneCanFailJobAndFetchAssignment() {
        String endUserToken = tokenFor("drone-fail-user", AuthRole.ENDUSER);
        String droneToken = tokenFor("drone-fail-drone", AuthRole.DRONE);

        OrderDtos.OrderResponse order = submitOrder(endUserToken, 15.0, 16.0, 17.0, 18.0);
        heartbeat(droneToken, 15.1, 16.1);
        reserveJob(droneToken, order.currentJobId());
        pickupJob(droneToken, order.currentJobId());

        DroneDtos.JobResponse failed = failJob(droneToken, order.currentJobId());
        Assertions.assertThat(failed.status()).isEqualTo(JobStatus.FAILED);

        ResponseEntity<DroneDtos.Assignment> assignmentResponse = restTemplate.exchange(
                "/drone/self/job",
                HttpMethod.GET,
                new HttpEntity<>(null, authHeaders(droneToken)),
                DroneDtos.Assignment.class
        );
        Assertions.assertThat(assignmentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void droneBrokenCreatesHandoffJobAndExcludesDrone() {
        String endUserToken = tokenFor("drone-broken-user", AuthRole.ENDUSER);
        String droneToken = tokenFor("drone-broken-drone", AuthRole.DRONE);

        OrderDtos.OrderResponse order = submitOrder(endUserToken, 21.0, 22.0, 23.0, 24.0);
        heartbeat(droneToken, 21.1, 22.1);
        reserveJob(droneToken, order.currentJobId());
        pickupJob(droneToken, order.currentJobId());

        ResponseEntity<DroneDtos.DroneResponse> brokenResponse = restTemplate.exchange(
                "/drone/self/broken",
                HttpMethod.POST,
                new HttpEntity<>(null, authHeaders(droneToken)),
                DroneDtos.DroneResponse.class
        );
        Assertions.assertThat(brokenResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<DroneDtos.JobResponse> openJobs = listOpenJobs(droneToken);
        DroneDtos.JobResponse handoff = openJobs.stream()
                .filter(job -> job.type() == JobType.HANDOFF_PICKUP_AND_DELIVER)
                .findFirst()
                .orElse(null);
        Assertions.assertThat(handoff).isNotNull();
        Assertions.assertThat(handoff.excludedDroneId()).isNotNull();

        ResponseEntity<String> reserveExcluded = restTemplate.exchange(
                "/drone/jobs/" + handoff.id() + "/reserve",
                HttpMethod.POST,
                new HttpEntity<>(null, authHeaders(droneToken)),
                String.class
        );
        Assertions.assertThat(reserveExcluded.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void concurrentReservationAllowsSingleWinner() throws Exception {
        String endUserToken = tokenFor("drone-concurrent-user", AuthRole.ENDUSER);
        OrderDtos.OrderResponse order = submitOrder(endUserToken, 31.0, 32.0, 33.0, 34.0);

        String droneTokenA = tokenFor("drone-concurrent-a", AuthRole.DRONE);
        String droneTokenB = tokenFor("drone-concurrent-b", AuthRole.DRONE);

        RestTemplate restTemplateA = new RestTemplateBuilder()
                .rootUri("http://localhost:" + port)
                .build();
        RestTemplate restTemplateB = new RestTemplateBuilder()
                .rootUri("http://localhost:" + port)
                .build();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Boolean> reserveA = () -> {
            ready.countDown();
            start.await(3, TimeUnit.SECONDS);
            HttpHeaders headers = authHeaders(droneTokenA);
            ResponseEntity<String> response = restTemplateA.exchange(
                    "/drone/jobs/" + order.currentJobId() + "/reserve",
                    HttpMethod.POST,
                    new HttpEntity<>(null, headers),
                    String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        };

        Callable<Boolean> reserveB = () -> {
            ready.countDown();
            start.await(3, TimeUnit.SECONDS);
            HttpHeaders headers = authHeaders(droneTokenB);
            ResponseEntity<String> response = restTemplateB.exchange(
                    "/drone/jobs/" + order.currentJobId() + "/reserve",
                    HttpMethod.POST,
                    new HttpEntity<>(null, headers),
                    String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        };

        Future<Boolean> futureA = executor.submit(reserveA);
        Future<Boolean> futureB = executor.submit(reserveB);

        ready.await(3, TimeUnit.SECONDS);
        start.countDown();

        boolean successA = futureA.get(5, TimeUnit.SECONDS);
        boolean successB = futureB.get(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        Assertions.assertThat(successA).isNotEqualTo(successB);
    }

    @Test
    void schedulerAssignsClosestDrone() {
        String endUserToken = tokenFor("scheduler-user", AuthRole.ENDUSER);
        OrderDtos.OrderResponse order = submitOrder(endUserToken, 41.0, 42.0, 43.0, 44.0);

        String droneNearToken = tokenFor("scheduler-drone-near", AuthRole.DRONE);
        String droneFarToken = tokenFor("scheduler-drone-far", AuthRole.DRONE);

        heartbeat(droneNearToken, 41.01, 42.01);
        heartbeat(droneFarToken, 60.0, 60.0);

        scheduler.assignJobsToClosestDrones();

        ResponseEntity<DroneDtos.Assignment> nearAssignment = restTemplate.exchange(
                "/drone/self/job",
                HttpMethod.GET,
                new HttpEntity<>(null, authHeaders(droneNearToken)),
                DroneDtos.Assignment.class
        );
        Assertions.assertThat(nearAssignment.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(nearAssignment.getBody()).isNotNull();
        Assertions.assertThat(nearAssignment.getBody().jobId()).isEqualTo(order.currentJobId());
        Assertions.assertThat(nearAssignment.getBody().jobStatus()).isEqualTo(JobStatus.RESERVED);

        ResponseEntity<DroneDtos.Assignment> farAssignment = restTemplate.exchange(
                "/drone/self/job",
                HttpMethod.GET,
                new HttpEntity<>(null, authHeaders(droneFarToken)),
                DroneDtos.Assignment.class
        );
        Assertions.assertThat(farAssignment.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(farAssignment.getBody()).isNull();
    }
}
