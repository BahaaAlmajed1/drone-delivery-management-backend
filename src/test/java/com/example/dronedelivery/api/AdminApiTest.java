package com.example.dronedelivery.api;

import com.example.dronedelivery.api.dto.DroneDtos;
import com.example.dronedelivery.api.dto.OrderDtos;
import com.example.dronedelivery.api.dto.common.Coordinates;
import com.example.dronedelivery.domain.DroneStatus;
import com.example.dronedelivery.security.AuthRole;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminApiTest extends ApiTestSupport {

    @Test
    void adminCanListBulkUpdateOrdersAndManageDrones() {
        String adminToken = tokenFor("admin-user", AuthRole.ADMIN);
        String endUserToken = tokenFor("admin-order-user", AuthRole.ENDUSER);

        OrderDtos.OrderResponse order = submitOrder(endUserToken, 40.0, 41.0, 42.0, 43.0);

        ResponseEntity<OrderDtos.OrderResponse[]> listResponse = restTemplate.exchange(
                "/admin/orders",
                HttpMethod.GET,
                new HttpEntity<>(null, authHeaders(adminToken)),
                OrderDtos.OrderResponse[].class
        );
        Assertions.assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(listResponse.getBody()).isNotNull();

        Map<String, List<UUID>> bulkReq = Map.of("orderIds", List.of(order.id()));
        ResponseEntity<OrderDtos.OrderResponse[]> bulkResponse = restTemplate.exchange(
                "/admin/orders/bulk",
                HttpMethod.POST,
                new HttpEntity<>(bulkReq, authHeaders(adminToken)),
                OrderDtos.OrderResponse[].class
        );
        Assertions.assertThat(bulkResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        OrderDtos.AdminUpdateOrderRequest updateReq = new OrderDtos.AdminUpdateOrderRequest(
                new Coordinates(50.0, 51.0),
                new Coordinates(52.0, 53.0)
        );
        ResponseEntity<OrderDtos.OrderResponse> updateResponse = restTemplate.exchange(
                "/admin/orders/" + order.id(),
                HttpMethod.POST,
                new HttpEntity<>(updateReq, authHeaders(adminToken)),
                OrderDtos.OrderResponse.class
        );
        Assertions.assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(updateResponse.getBody()).isNotNull();
        Assertions.assertThat(updateResponse.getBody().origin().lat()).isEqualTo(50.0);

        tokenFor("admin-list-drone", AuthRole.DRONE);

        ResponseEntity<DroneDtos.DroneResponse[]> dronesResponse = restTemplate.exchange(
                "/admin/drones",
                HttpMethod.GET,
                new HttpEntity<>(null, authHeaders(adminToken)),
                DroneDtos.DroneResponse[].class
        );
        Assertions.assertThat(dronesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(dronesResponse.getBody()).isNotNull();

        DroneDtos.DroneResponse existingDrone = dronesResponse.getBody()[0];
        Map<String, DroneStatus> statusReq = Map.of("status", DroneStatus.BROKEN);
        ResponseEntity<DroneDtos.DroneResponse> brokenResponse = restTemplate.exchange(
                "/admin/drones/" + existingDrone.id() + "/status",
                HttpMethod.POST,
                new HttpEntity<>(statusReq, authHeaders(adminToken)),
                DroneDtos.DroneResponse.class
        );
        Assertions.assertThat(brokenResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, DroneStatus> fixedReq = Map.of("status", DroneStatus.FIXED);
        ResponseEntity<DroneDtos.DroneResponse> fixedResponse = restTemplate.exchange(
                "/admin/drones/" + existingDrone.id() + "/status",
                HttpMethod.POST,
                new HttpEntity<>(fixedReq, authHeaders(adminToken)),
                DroneDtos.DroneResponse.class
        );
        Assertions.assertThat(fixedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
