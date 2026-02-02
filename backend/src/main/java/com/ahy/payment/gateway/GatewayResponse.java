package com.ahy.payment.gateway;

import com.ahy.payment.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GatewayResponse {
    private PaymentStatus status;      // mapped to our PaymentStatus enum
    private String externalId;         // PSP transaction id (payment_intent id, charge id...)
    private String rawResponse;        // optional: raw JSON or message for debugging
}
