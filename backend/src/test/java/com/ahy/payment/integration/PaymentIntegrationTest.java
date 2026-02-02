package com.ahy.payment.integration;

import com.ahy.payment.dto.request.AuthorizeRequest;
import com.ahy.payment.dto.response.TransactionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "RUN_INTEGRATION", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mock")
class PaymentIntegrationTest {

    // Start container manually inside DynamicPropertySource. If Docker is not available, fall back to H2 in-memory DB.
    static org.testcontainers.containers.PostgreSQLContainer<?> postgres;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        boolean dockerAvailable = org.testcontainers.DockerClientFactory.instance().isDockerAvailable();
        if (dockerAvailable) {
            postgres = new org.testcontainers.containers.PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");
            postgres.start();

            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        } else {
            // Fallback to H2 in-memory for environments without Docker (local dev without Docker)
            registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false");
            registry.add("spring.datasource.username", () -> "sa");
            registry.add("spring.datasource.password", () -> "");
        }

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("payments.mock.disableRandom", () -> "true");
        registry.add("payments.stripe.webhookSecret", () -> "whsec_test");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void authorize_capture_refund_flow() {
        AuthorizeRequest request = new AuthorizeRequest();
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("USD");
        request.setIdempotencyKey("idem-integ-1");
        request.setMetadata(Map.of("orderId", "ORD-123"));

        var createResp = restTemplate.postForEntity("/payments/authorize", request, TransactionResponse.class);
        assertThat(createResp.getStatusCode().is2xxSuccessful()).isTrue();
        TransactionResponse created = createResp.getBody();
        assertThat(created).isNotNull();
        assertThat(created.getStatus()).isEqualTo(com.ahy.payment.model.PaymentStatus.AUTHORIZED);

        var captureResp = restTemplate.postForEntity("/payments/" + created.getId() + "/capture", null, TransactionResponse.class);
        assertThat(captureResp.getStatusCode().is2xxSuccessful()).isTrue();
        TransactionResponse captured = captureResp.getBody();
        assertThat(captured).isNotNull();
        assertThat(captured.getStatus()).isEqualTo(com.ahy.payment.model.PaymentStatus.CAPTURED);

        var refundResp = restTemplate.postForEntity("/payments/" + created.getId() + "/refund", null, TransactionResponse.class);
        assertThat(refundResp.getStatusCode().is2xxSuccessful()).isTrue();
        TransactionResponse refunded = refundResp.getBody();
        assertThat(refunded).isNotNull();
        assertThat(refunded.getStatus()).isEqualTo(com.ahy.payment.model.PaymentStatus.REFUNDED);
    }
}
