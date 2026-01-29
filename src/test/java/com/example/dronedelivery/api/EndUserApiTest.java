package com.example.dronedelivery.api;

import com.example.dronedelivery.api.dto.OrderDtos;
import com.example.dronedelivery.security.AuthRole;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EndUserApiTest extends ApiTestSupport {

    @Test
    void endUserCanSubmitWithdrawAndViewOrders() {
        String token = tokenFor("enduser-basic", AuthRole.ENDUSER);
        OrderDtos.OrderResponse order = submitOrder(token, 10.0, 20.0, 30.0, 40.0);

        ResponseEntity<OrderDtos.OrderResponse> withdrawResponse = restTemplate.exchange(
                "/enduser/orders/" + order.id() + "/withdraw",
                HttpMethod.POST,
                new HttpEntity<>(null, authHeaders(token)),
                OrderDtos.OrderResponse.class
        );
        Assertions.assertThat(withdrawResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(withdrawResponse.getBody()).isNotNull();
        Assertions.assertThat(withdrawResponse.getBody().status()).isEqualTo(com.example.dronedelivery.domain.OrderStatus.CANCELED);

        ResponseEntity<OrderDtos.OrderResponse[]> listResponse = restTemplate.exchange(
                "/enduser/orders",
                HttpMethod.GET,
                new HttpEntity<>(null, authHeaders(token)),
                OrderDtos.OrderResponse[].class
        );
        Assertions.assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(listResponse.getBody()).isNotNull();
        Assertions.assertThat(listResponse.getBody()).extracting(OrderDtos.OrderResponse::id).contains(order.id());

        ResponseEntity<OrderDtos.OrderResponse> getResponse = restTemplate.exchange(
                "/enduser/orders/" + order.id(),
                HttpMethod.GET,
                new HttpEntity<>(null, authHeaders(token)),
                OrderDtos.OrderResponse.class
        );
        Assertions.assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(getResponse.getBody()).isNotNull();
    }

    @Test
    void endUserCannotWithdrawAfterPickup() {
        String endUserToken = tokenFor("enduser-withdraw", AuthRole.ENDUSER);
        String droneToken = tokenFor("enduser-withdraw-drone", AuthRole.DRONE);
        OrderDtos.OrderResponse order = submitOrder(endUserToken, 1.0, 2.0, 3.0, 4.0);

        heartbeat(droneToken, 1.1, 2.1);
        reserveJob(droneToken, order.currentJobId());
        pickupJob(droneToken, order.currentJobId());

        ResponseEntity<String> withdrawResponse = restTemplate.exchange(
                "/enduser/orders/" + order.id() + "/withdraw",
                HttpMethod.POST,
                new HttpEntity<>(null, authHeaders(endUserToken)),
                String.class
        );
        Assertions.assertThat(withdrawResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
