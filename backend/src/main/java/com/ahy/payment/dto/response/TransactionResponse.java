package com.ahy.payment.dto.response;

import com.ahy.payment.model.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data

public class TransactionResponse {

    private UUID id;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private Instant createdAt;
}

