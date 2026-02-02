package com.ahy.payment.gateway.impl;

import com.ahy.payment.gateway.GatewayResponse;
import com.ahy.payment.gateway.PaymentGateway;
import com.ahy.payment.model.PaymentStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Profile("mock") // active when spring profile is "mock"
public class MockPaymentGatewayImpl implements PaymentGateway {

    @org.springframework.beans.factory.annotation.Value("${payments.mock.disableRandom:false}")
    private boolean disableRandom;

    @Override
    public GatewayResponse authorize(BigDecimal amount, String currency, Map<String, String> metadata) {
        // Simulate random failure (10%) unless disabled for tests
        if (!disableRandom) {
            int randomFail = ThreadLocalRandom.current().nextInt(1, 11);
            if (randomFail == 1) {
                return new GatewayResponse(PaymentStatus.FAILED, null, "Simulated PSP failure");
            }
        }

        if (amount.compareTo(new BigDecimal("1000")) > 0) {
            return new GatewayResponse(PaymentStatus.DECLINED, null, "Amount exceeds limit");
        }

        // success
        String externalId = "mock_" + UUID.randomUUID();
        return new GatewayResponse(PaymentStatus.AUTHORIZED, externalId, "Mock authorized");
    }

    @Override
    public GatewayResponse capture(String externalId, BigDecimal amount) {
        // Accept any externalId starting with mock_
        if (externalId == null || !externalId.startsWith("mock_")) {
            return new GatewayResponse(PaymentStatus.FAILED, externalId, "Invalid external id");
        }
        return new GatewayResponse(PaymentStatus.CAPTURED, externalId, "Mock captured");
    }

    @Override
    public GatewayResponse refund(String externalId, BigDecimal amount) {
        if (externalId == null || !externalId.startsWith("mock_")) {
            return new GatewayResponse(PaymentStatus.FAILED, externalId, "Invalid external id");
        }
        return new GatewayResponse(PaymentStatus.REFUNDED, externalId, "Mock refunded");
    }
}
