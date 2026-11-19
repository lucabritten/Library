package de.htwsaar.library.infrastructure;

import de.htwsaar.library.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorPortSecurityIT extends IntegrationTest {

    @LocalManagementPort
    private int managementPort;

   private RestTestClient management;

   @Autowired
   private RestTestClient client;

   @BeforeEach
   public void setup() {
       management = RestTestClient.bindToServer()
               .baseUrl("http://localhost:" + managementPort)
               .build();
   }

    @Test
    void prometheus_onAppPort_returns404() {
        client.get().uri("/actuator/prometheus")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void prometheus_onManagementPort_isPublicWithHistograms() {
       client.get().uri(BASE_URI + "/books").exchange().expectStatus().isOk();

        String body = management.get().uri("/actuator/prometheus")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        assertThat(body.lines())
                .filteredOn(l -> l.startsWith("http_server_requests_seconds_bucket"))
                .filteredOn(l -> l.contains("uri=\"/api/v1/books\""))
                .isNotEmpty()
                .anyMatch(l -> l.contains("le=\"+Inf\""));
    }

    @Test
    void liveness_onManagementPort_returns200() {
        management.get().uri("/actuator/health/liveness")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody().contains("status");
    }

    @Test
    void env_onManagementPort_isNotExposed() {
        management.get().uri("/actuator/env")
                .headers(h -> h.setBearerAuth(adminToken()))
                .exchange()
                .expectStatus().isNotFound();
    }
}
