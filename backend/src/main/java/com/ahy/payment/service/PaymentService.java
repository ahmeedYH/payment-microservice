package com.ahy.payment.service;

import com.ahy.payment.model.Transaction;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface PaymentService {

    // Authorize a payment (authorize only, no capture)
    Transaction authorizePayment(BigDecimal amount, String currency, Map<String, String> metadata, String idempotencyKey);

    // Get a transaction by ID
    Transaction getTransaction(UUID id);

    // Capture a previously authorized payment
    Transaction capturePayment(UUID id);

    // Refund a captured payment
    Transaction refundPayment(UUID id);

    /* =======================
       Webhook internal helpers
       ======================= */
    void handlePaymentIntentSucceeded(String paymentIntentId);
    void handlePaymentIntentFailed(String paymentIntentId);
    void handleChargeRefunded(String paymentIntentId);

    
}

