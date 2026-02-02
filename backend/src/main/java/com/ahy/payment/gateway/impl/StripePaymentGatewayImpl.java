package com.ahy.payment.gateway.impl;

import com.ahy.payment.gateway.GatewayResponse;
import com.ahy.payment.gateway.PaymentGateway;
import com.ahy.payment.gateway.WebhookGateway;
import com.ahy.payment.model.PaymentStatus;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.ahy.payment.exception.InvalidSignatureException;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Map;

@Service
@Profile("stripe") // active when spring profile is "stripe"
public class StripePaymentGatewayImpl implements PaymentGateway , WebhookGateway {

    @Value("${stripe.secret-key}")
    private String stripeApiKey;

    @Value("${payments.stripe.webhookSecret}")
    private String webhookSecret;


    @PostConstruct
    public void init() {
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            // in production fail fast; here we log
            System.err.println("STRIPE API KEY is not set (payments.stripe.apiKey). Stripe gateway will fail.");
        } else {
            Stripe.apiKey = stripeApiKey;
        }
    }

    @Override
    public GatewayResponse authorize(BigDecimal amount, String currency, Map<String, String> metadata) {
        try {
            // Stripe expects amount in smallest currency unit (e.g., cents)
            long amountInMinor = toMinorUnits(amount, currency);

            PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInMinor)
                    .setCurrency(currency.toLowerCase())
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL) // authorize only
                    .putAllMetadata(metadata == null ? Map.of() : metadata);

            PaymentIntent intent = PaymentIntent.create(builder.build());

            // PaymentIntent status may be requires_confirmation, requires_action, etc.
            // We'll map common statuses: 'requires_capture' or 'requires_payment_method' etc.
            String intentStatus = intent.getStatus(); // e.g., "requires_capture", "requires_payment_method", "succeeded"
            PaymentStatus mapped = mapStripeStatusToPaymentStatus(intentStatus);

            return new GatewayResponse(mapped, intent.getId(), intent.toJson());
        } catch (StripeException ex) {
            return new GatewayResponse(PaymentStatus.FAILED, null, ex.getMessage());
        }
    }

    @Override
    public GatewayResponse capture(String externalId, BigDecimal amount) {
        try {
            // capture the payment intent
            PaymentIntent intent = PaymentIntent.retrieve(externalId);

            PaymentIntentCaptureParams.Builder capBuilder = PaymentIntentCaptureParams.builder();
            if (amount != null) {
                long minor = toMinorUnits(amount, intent.getCurrency());
                capBuilder.setAmountToCapture(minor);
            }

            PaymentIntent captured = intent.capture(capBuilder.build());
            PaymentStatus mapped = mapStripeStatusToPaymentStatus(captured.getStatus());
            return new GatewayResponse(mapped, captured.getId(), captured.toJson());
        } catch (StripeException ex) {
            return new GatewayResponse(PaymentStatus.FAILED, externalId, ex.getMessage());
        }
    }

    @Override
    public GatewayResponse refund(String externalId, BigDecimal amount) {
        try {
            // 1. Retrieve PaymentIntent
            PaymentIntent intent = PaymentIntent.retrieve(externalId);

            // 2. Fetch charges linked to this PaymentIntent
            ChargeCollection charges = Charge.list(
                    Map.of("payment_intent", intent.getId())
            );

            if (charges.getData().isEmpty()) {
                return new GatewayResponse(
                        PaymentStatus.FAILED,
                        externalId,
                        "No charge found to refund"
                );
            }

            // 3. Take first charge
            String chargeId = charges.getData().get(0).getId();

            // 4. Create refund
            RefundCreateParams params = RefundCreateParams.builder()
                    .setCharge(chargeId)
                    .build();

            Refund refund = Refund.create(params);

            // 5. Map status
            return new GatewayResponse(
                    PaymentStatus.REFUNDED,
                    externalId,
                    refund.toJson()
            );

        } catch (StripeException ex) {
            return new GatewayResponse(
                    PaymentStatus.FAILED,
                    externalId,
                    ex.getMessage()
            );
        }
    }


    // Helper: convert major units to minor units (e.g., dollars -> cents)
    private long toMinorUnits(BigDecimal amount, String currency) {
        // For simplicity assume 2 decimal currencies (USD, EUR). For real app use a currency library.
        BigDecimal minor = amount.multiply(BigDecimal.valueOf(100));
        return minor.longValue();
    }

    private PaymentStatus mapStripeStatusToPaymentStatus(String stripeStatus) {
        if (stripeStatus == null) return PaymentStatus.FAILED;
        switch (stripeStatus) {
            case "requires_capture":
            case "requires_confirmation":
            case "requires_action":
            case "processing":
                return PaymentStatus.AUTHORIZED;
            case "succeeded":
                return PaymentStatus.CAPTURED;
            case "canceled":
                return PaymentStatus.DECLINED;
            default:
                return PaymentStatus.FAILED;
        }
    }

    @Override
    public Event parseAndVerify(String payload, String signature) {
        try {
            return Webhook.constructEvent(
                    payload,
                    signature,
                    webhookSecret
            );
        } catch (SignatureVerificationException e) {
            throw new InvalidSignatureException(e.getMessage());
        }
    }
}
