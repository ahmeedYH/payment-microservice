package com.ahy.payment.webhook;

import com.ahy.payment.model.PaymentStatus;
import com.ahy.payment.model.Transaction;
import com.ahy.payment.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "RUN_INTEGRATION", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mock")
@TestPropertySource(properties = "payments.stripe.webhookSecret=whsec_test")
public class StripeWebhookE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransactionRepository transactionRepository;

    private static String computeStripeSignatureHeader(String payload, String secret) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000L;
        String signedPayload = timestamp + "." + payload;

        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] macData = sha256_HMAC.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        for (byte b : macData) {
            sb.append(String.format("%02x", b));
        }
        String signature = sb.toString();
        return "t=" + timestamp + ",v1=" + signature;
    }

    @Test
    void stripeWebhook_validSignature_shouldUpdateTransactionToCaptured() throws Exception {
        // Create transaction with externalTransactionId matching the payment_intent id
        Transaction tx = new Transaction();
        tx.setAmount(BigDecimal.valueOf(42));
        tx.setCurrency("USD");
        tx.setExternalTransactionId("pi_test_123");
        tx.setStatus(PaymentStatus.AUTHORIZED);
        tx.setIdempotencyKey("idem-webhook-1");
        transactionRepository.saveAndFlush(tx);
        // ensure the transaction is persisted and queryable before webhook delivery
        assertThat(transactionRepository.findByExternalTransactionId("pi_test_123")).isPresent();

        String payload = "{\n" +
                "  \"id\": \"evt_test\",\n" +
                "  \"object\": \"event\",\n" +
                "  \"type\": \"payment_intent.succeeded\",\n" +
                "  \"data\": {\n" +
                "    \"object\": {\n" +
                "      \"id\": \"pi_test_123\",\n" +
                "      \"object\": \"payment_intent\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        String sigHeader = computeStripeSignatureHeader(payload, "whsec_test");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Stripe-Signature", sigHeader);

        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        var resp = restTemplate.postForEntity("/payments/webhooks/stripe", request, Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        Transaction updated = transactionRepository.findByExternalTransactionId("pi_test_123").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
    }

    @Test
    void stripeWebhook_invalidSignature_shouldReturnBadRequest() throws Exception {
        Transaction tx = new Transaction();
        tx.setAmount(BigDecimal.valueOf(50));
        tx.setCurrency("USD");
        tx.setExternalTransactionId("pi_test_456");
        tx.setStatus(PaymentStatus.AUTHORIZED);
        tx.setIdempotencyKey("idem-webhook-2");
        transactionRepository.save(tx);

        String payload = "{\n" +
                "  \"id\": \"evt_test2\",\n" +
                "  \"object\": \"event\",\n" +
                "  \"type\": \"payment_intent.succeeded\",\n" +
                "  \"data\": {\n" +
                "    \"object\": {\n" +
                "      \"id\": \"pi_test_456\",\n" +
                "      \"object\": \"payment_intent\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        // Wrong secret
        String sigHeader = computeStripeSignatureHeader(payload, "wrong_secret");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Stripe-Signature", sigHeader);

        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        var resp = restTemplate.postForEntity("/payments/webhooks/stripe", request, Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Transaction unchanged = transactionRepository.findByExternalTransactionId("pi_test_456").orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    }
}
