package com.ahy.payment.util;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import org.springframework.stereotype.Component;

@Component
public class StripeWebhookVerifier {

    public com.ahy.payment.util.WebhookEvent verifyStripeSignature(String payload, String signatureHeader, String secret) throws SignatureVerificationException {
        Event event = SignatureUtil.verifyStripeSignature(payload, signatureHeader, secret);

        String paymentIntentId = extractPaymentIntentId(event);

        // Log for observability in tests to ease debugging
        org.slf4j.LoggerFactory.getLogger(StripeWebhookVerifier.class).info("Verified event type={} paymentIntentId={}", event.getType(), paymentIntentId);

        return new com.ahy.payment.util.WebhookEvent(event.getType(), paymentIntentId);
    }

    /*
     * Package-private helper to extract the payment intent id from various shapes of the event data object.
     * Keeping this package-private makes it easy to test different scenarios without relying on static plumbing.
     */
    String extractPaymentIntentId(Event event) {
        Object dataObject = event.getData().getObject();
        String paymentIntentId = null;

        if (dataObject instanceof PaymentIntent) {
            paymentIntentId = ((PaymentIntent) dataObject).getId();
        } else if (dataObject instanceof com.stripe.model.StripeObject) {
            // StripeObject may not expose getters consistently across SDK versions; parse JSON safely.
            try {
                String json = ((com.stripe.model.StripeObject) dataObject).toJson();
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> map = mapper.readValue(json, java.util.Map.class);
                paymentIntentId = (String) map.get("id");
            } catch (Exception ignored) {
            }
        } else if (dataObject instanceof com.google.gson.JsonObject) {
            com.google.gson.JsonObject jsonObj = (com.google.gson.JsonObject) dataObject;
            if (jsonObj.has("id")) {
                try {
                    paymentIntentId = jsonObj.get("id").getAsString();
                } catch (Exception ignored) {
                }
            }
        } else if (dataObject instanceof java.util.Map) {
            paymentIntentId = (String) ((java.util.Map) dataObject).get("id");
        } else {
            // Fallback: try using the data object deserializer if available. Use reflection so tests
            // don't rely on Stripe SDK internal types being present.
            try {
                java.lang.reflect.Method m = event.getClass().getMethod("getDataObjectDeserializer");
                Object deser = m.invoke(event);
                if (deser != null) {
                    java.lang.reflect.Method getObject = deser.getClass().getMethod("getObject");
                    Object opt = getObject.invoke(deser);
                    if (opt instanceof java.util.Optional) {
                        Object maybeIntent = ((java.util.Optional) opt).orElse(null);
                        if (maybeIntent instanceof PaymentIntent) {
                            paymentIntentId = ((PaymentIntent) maybeIntent).getId();
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (paymentIntentId == null) {
            throw new IllegalArgumentException("Could not extract payment intent id from event");
        }

        return paymentIntentId;
    }
}
