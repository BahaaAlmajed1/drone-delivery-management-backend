package com.example.dronedelivery.api;

import com.example.dronedelivery.api.dto.AuthDtos;
import com.example.dronedelivery.api.dto.DroneDtos;
import com.example.dronedelivery.api.dto.OrderDtos;
import com.example.dronedelivery.api.dto.common.Coordinates;
import com.example.dronedelivery.security.AuthRole;
import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.UUID;

public abstract class ApiTestSupport {

    @Autowired
    protected TestRestTemplate restTemplate;

    protected String tokenFor(String name, AuthRole role) {
        var req = new AuthDtos.TokenRequest(name, role);
        var response = restTemplate.postForEntity("/auth/token", req, AuthDtos.TokenResponse.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        return response.getBody().accessToken();
    }

    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    protected OrderDtos.OrderResponse submitOrder(String token, double oLat, double oLng, double dLat, double dLng) {
        var req = new OrderDtos.SubmitOrderRequest(
                new Coordinates(oLat, oLng),
                new Coordinates(dLat, dLng)
        );
        var response = restTemplate.exchange(
                "/enduser/orders",
                HttpMethod.POST,
                new HttpEntity<>(req, authHeaders(token)),
                OrderDtos.OrderResponse.class
        );
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    protected DroneDtos.JobResponse reserveJob(String token, UUID jobId) {
        var response = restTemplate.exchange(
                "/drone/jobs/" + jobId + "/reserve",
                HttpMethod.POST,
                new HttpEntity<>(null, authHeaders(token)),
                DroneDtos.JobResponse.class
        );
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    protected DroneDtos.JobResponse pickupJob(String token, UUID jobId) {
        var response = restTemplate.exchange(
                "/drone/jobs/" + jobId + "/pickup",
                HttpMethod.POST,
                new HttpEntity<>(null, authHeaders(token)),
                DroneDtos.JobResponse.class
        );
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    protected DroneDtos.JobResponse completeJob(String token, UUID jobId) {
        var response = restTemplate.exchange(
                "/drone/jobs/" + jobId + "/complete",
                HttpMethod.POST,
                new HttpEntity<>(null, authHeaders(token)),
                DroneDtos.JobResponse.class
        );
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    protected DroneDtos.JobResponse failJob(String token, UUID jobId) {
        var response = restTemplate.exchange(
                "/drone/jobs/" + jobId + "/fail",
                HttpMethod.POST,
                new HttpEntity<>(null, authHeaders(token)),
                DroneDtos.JobResponse.class
        );
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    protected DroneDtos.HeartbeatResponse heartbeat(String token, double lat, double lng) {
        var req = new DroneDtos.HeartbeatRequest(new Coordinates(lat, lng));
        var response = restTemplate.exchange(
                "/drone/self/heartbeat",
                HttpMethod.POST,
                new HttpEntity<>(req, authHeaders(token)),
                DroneDtos.HeartbeatResponse.class
        );
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    protected List<DroneDtos.JobResponse> listOpenJobs(String token) {
        var response = restTemplate.exchange(
                "/drone/jobs/open",
                HttpMethod.GET,
                new HttpEntity<>(null, authHeaders(token)),
                new ParameterizedTypeReference<List<DroneDtos.JobResponse>>() {
                }
        );
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }
}
