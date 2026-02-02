package com.ahy.payment.repository;

import com.ahy.payment.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    Optional<Transaction> findByExternalTransactionId(String externalTransactionId);

}
