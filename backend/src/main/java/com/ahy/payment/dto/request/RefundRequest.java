package com.ahy.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public class RefundRequest {

    @NotNull(message = "Transaction ID is required")
    private UUID transactionId;
    @NotNull(message = "Amount is required")
    @Positive(message = "Refund amount must be greater than zero")
    private BigDecimal amount;
}
