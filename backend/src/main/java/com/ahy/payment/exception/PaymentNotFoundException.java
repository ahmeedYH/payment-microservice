package com.ahy.payment.exception;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(UUID transactionId) {
        super("Payment transaction not found with id: " + transactionId);
    }

    public PaymentNotFoundException(String externalTransactionId) {
        super("Payment transaction not found with external id: " + externalTransactionId);
    }
}
