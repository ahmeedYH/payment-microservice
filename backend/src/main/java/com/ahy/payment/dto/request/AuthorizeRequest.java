package com.ahy.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;
import jakarta.validation.constraints.NotNull;

@Data

public class AuthorizeRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be ISO-4217 (3 letters)")
    private String currency;
    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;


    private Map<String, String> metadata;
    // Optional key-value metadata (orderId, device, etc.)
}
