package com.ahy.payment.util;

import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StripeWebhookVerifierTest {

    private final StripeWebhookVerifier verifier = new StripeWebhookVerifier();

    @Test
    void extract_fromPaymentIntent() {
        String json = "{\n" +
                "  \"id\": \"evt_test\",\n" +
                "  \"object\": \"event\",\n" +
                "  \"type\": \"payment_intent.succeeded\",\n" +
                "  \"data\": {\n" +
                "    \"object\": {\n" +
                "      \"id\": \"pi_test_1\",\n" +
                "      \"object\": \"payment_intent\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        Event event = Event.GSON.fromJson(json, Event.class);

        assertThat(verifier.extractPaymentIntentId(event)).isEqualTo("pi_test_1");
    }

    @Test
    void extract_fromMap() {
        String json = "{\n" +
                "  \"id\": \"evt_test2\",\n" +
                "  \"object\": \"event\",\n" +
                "  \"type\": \"payment_intent.succeeded\",\n" +
                "  \"data\": {\n" +
                "    \"object\": {\n" +
                "      \"id\": \"pi_map_1\",\n" +
                "      \"object\": \"payment_intent\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        Event event = Event.GSON.fromJson(json, Event.class);

        assertThat(verifier.extractPaymentIntentId(event)).isEqualTo("pi_map_1");
    }

    @Test
    void extract_fromStripeObjectCharge() {
        String json = "{\n" +
                "  \"id\": \"evt_test3\",\n" +
                "  \"object\": \"event\",\n" +
                "  \"type\": \"charge.refunded\",\n" +
                "  \"data\": {\n" +
                "    \"object\": {\n" +
                "      \"id\": \"chg_123\",\n" +
                "      \"object\": \"charge\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        Event event = Event.GSON.fromJson(json, Event.class);

        // Charge-like object represented as JsonObject should be processed and id returned
        assertThat(verifier.extractPaymentIntentId(event)).isEqualTo("chg_123");
    }

    @Test
    void extract_missingId_throws() {
        String json = "{\n" +
                "  \"id\": \"evt_test4\",\n" +
                "  \"object\": \"event\",\n" +
                "  \"type\": \"unknown.event\",\n" +
                "  \"data\": {\n" +
                "    \"object\": {\n" +
                "      \"object\": \"payment_intent\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        Event event = Event.GSON.fromJson(json, Event.class);

        assertThatThrownBy(() -> verifier.extractPaymentIntentId(event)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Could not extract payment intent id");
    }

    @Test
    void extract_fromDataObjectDeserializerObject() {
        PaymentIntent intent = new PaymentIntent();
        intent.setId("pi_fallback_2");

        Object deser = new Object() {
            public java.util.Optional getObject() {
                return java.util.Optional.of(intent);
            }
        };

        String id = verifier.extractPaymentIntentIdFromDeserializerObject(deser);
        assertThat(id).isEqualTo("pi_fallback_2");
    }


}
