package com.ahy.payment.service;

import com.ahy.payment.exception.PaymentNotFoundException;
import com.ahy.payment.gateway.GatewayResponse;
import com.ahy.payment.gateway.PaymentGateway;
import com.ahy.payment.model.PaymentStatus;
import com.ahy.payment.model.Transaction;
import com.ahy.payment.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final TransactionRepository repository;
    private final PaymentGateway gateway;

    public PaymentServiceImpl(TransactionRepository repository, PaymentGateway gateway) {
        this.repository = repository;
        this.gateway = gateway;
    }

    @Override
    public Transaction authorizePayment(BigDecimal amount, String currency, Map<String, String> metadata, String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> {
                    Transaction transaction = new Transaction();
                    transaction.setAmount(amount);
                    transaction.setCurrency(currency);
                    transaction.setMetadata(metadata);
                    transaction.setIdempotencyKey(idempotencyKey);

                    GatewayResponse resp = gateway.authorize(amount, currency, metadata);

                    transaction.setStatus(resp.getStatus());
                    transaction.setExternalTransactionId(resp.getExternalId());

                    return repository.save(transaction);
                });
    }

    @Override
    public Transaction getTransaction(UUID id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public Transaction capturePayment(UUID id) {
        Transaction tx = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + id));

        if (tx.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new RuntimeException("Only AUTHORIZED payments can be captured.");
        }

        GatewayResponse resp = gateway.capture(tx.getExternalTransactionId(), tx.getAmount());
        tx.setStatus(resp.getStatus());
        return repository.save(tx);
    }

    @Override
    public Transaction refundPayment(UUID id) {
        Transaction tx = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + id));

        if (tx.getStatus() != PaymentStatus.CAPTURED) {
            throw new RuntimeException("Only CAPTURED payments can be refunded.");
        }

        GatewayResponse resp = gateway.refund(tx.getExternalTransactionId(), tx.getAmount());
        tx.setStatus(resp.getStatus());
        return repository.save(tx);
    }

    /* =======================
       Webhook helpers
       ======================= */
    @Override
    public void handlePaymentIntentSucceeded(String paymentIntentId) {
        Transaction tx = repository.findByExternalTransactionId(paymentIntentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentIntentId));

        if (tx.getStatus() != PaymentStatus.CAPTURED) {
            tx.setStatus(PaymentStatus.CAPTURED);
            repository.save(tx);
        }
    }

    @Override
    public void handlePaymentIntentFailed(String paymentIntentId) {
        Transaction tx = repository.findByExternalTransactionId(paymentIntentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentIntentId));

        if (tx.getStatus() != PaymentStatus.FAILED) {
            tx.setStatus(PaymentStatus.FAILED);
            repository.save(tx);
        }
    }

    @Override
    public void handleChargeRefunded(String paymentIntentId) {
        Transaction tx = repository.findByExternalTransactionId(paymentIntentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentIntentId));

        if (tx.getStatus() != PaymentStatus.REFUNDED) {
            tx.setStatus(PaymentStatus.REFUNDED);
            repository.save(tx);
        }
    }
}
