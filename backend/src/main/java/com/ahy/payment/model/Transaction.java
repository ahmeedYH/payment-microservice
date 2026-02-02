package com.ahy.payment.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Entity
@Table(name = "transaction")

public class Transaction {

    @Id
    @GeneratedValue
    private UUID id;

    private BigDecimal amount;

    private String currency;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String externalTransactionId;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> metadata;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    @Lob
    private String gatewayResponse; // store raw JSON from PSP for debugging/audit
    @Column(name = "idempotency_key", unique = true, nullable = false)
    private String idempotencyKey;
}
