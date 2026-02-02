package com.ahy.payment.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CaptureRequest {
    @NotNull(message = "Transaction ID is required")
    private UUID transactionId;
}
