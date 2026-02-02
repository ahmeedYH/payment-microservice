package com.ahy.payment.gateway;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface PaymentGateway {

    /**
     * Authorize payment at the gateway.
     * @param amount in major currency unit (e.g. 10.50 means 10.50 USD)
     * @param currency currency code (USD, EUR...)
     * @param metadata key-value metadata
     * @return GatewayResponse (status + external id)
     */
    GatewayResponse authorize(BigDecimal amount, String currency, Map<String, String> metadata);

    /**
     * Capture a previously authorized payment.
     * @param externalId id returned by gateway when authorize was called (PSP id)
     * @param amount amount to capture (nullable if full)
     * @return GatewayResponse
     */
    GatewayResponse capture(String externalId, BigDecimal amount);

    /**
     * Refund a captured payment.
     * @param externalId the gateway's id for the captured transaction
     * @param amount amount to refund (nullable for full)
     * @return GatewayResponse
     */
    GatewayResponse refund(String externalId, BigDecimal amount);
}
