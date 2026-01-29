package com.example.dronedelivery.api;

import com.example.dronedelivery.api.dto.AuthDtos;
import com.example.dronedelivery.security.AuthRole;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthApiTest {

    @LocalServerPort
    int port;

    @Test
    void tokenIssuedForEachRole() {
        TestRestTemplate restTemplate = new TestRestTemplate();
        ResponseEntity<AuthDtos.TokenResponse> adminResponse = restTemplate
                .postForEntity("http://localhost:" + port + "/auth/token",
                        new AuthDtos.TokenRequest("auth-admin", AuthRole.ADMIN),
                        AuthDtos.TokenResponse.class);
        ResponseEntity<AuthDtos.TokenResponse> endUserResponse = restTemplate
                .postForEntity("http://localhost:" + port + "/auth/token",
                        new AuthDtos.TokenRequest("auth-enduser", AuthRole.ENDUSER),
                        AuthDtos.TokenResponse.class);
        ResponseEntity<AuthDtos.TokenResponse> droneResponse = restTemplate
                .postForEntity("http://localhost:" + port + "/auth/token",
                        new AuthDtos.TokenRequest("auth-drone", AuthRole.DRONE),
                        AuthDtos.TokenResponse.class);

        Assertions.assertThat(adminResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(endUserResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(droneResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        Assertions.assertThat(adminResponse.getBody()).isNotNull();
        Assertions.assertThat(endUserResponse.getBody()).isNotNull();
        Assertions.assertThat(droneResponse.getBody()).isNotNull();

        Assertions.assertThat(adminResponse.getBody().accessToken()).isNotBlank();
        Assertions.assertThat(endUserResponse.getBody().accessToken()).isNotBlank();
        Assertions.assertThat(droneResponse.getBody().accessToken()).isNotBlank();
    }
}
