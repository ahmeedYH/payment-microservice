package com.ahy.payment.service;

import com.ahy.payment.gateway.GatewayResponse;
import com.ahy.payment.gateway.PaymentGateway;
import com.ahy.payment.model.PaymentStatus;
import com.ahy.payment.model.Transaction;
import com.ahy.payment.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID; 

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceImplTest {

    private TransactionRepository repository;
    private PaymentGateway gateway;
    private PaymentService service; // interface reference

    @BeforeEach
    void setUp() {
        repository = mock(TransactionRepository.class);
        gateway = mock(PaymentGateway.class);
        service = new PaymentServiceImpl(repository, gateway); // ✅ implementation
    }

    @Test
    void testAuthorizePayment_Success() {

        Map<String, String> metadata = new HashMap<>();
        metadata.put("orderId", "ORD-1");

        when(repository.findByIdempotencyKey(anyString()))
                .thenReturn(Optional.empty());

        when(gateway.authorize(any(), anyString(), any()))
                .thenReturn(new GatewayResponse(
                        PaymentStatus.AUTHORIZED,
                        "ext-123",
                        null
                ));

        when(repository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Transaction result = service.authorizePayment(
                new BigDecimal("500"),
                "USD",
                metadata,
                "idem-key-123"
        );

        assertEquals(PaymentStatus.AUTHORIZED, result.getStatus());
        assertEquals("USD", result.getCurrency());
        assertEquals("idem-key-123", result.getIdempotencyKey());
        assertEquals("ext-123", result.getExternalTransactionId());

        verify(gateway, times(1))
                .authorize(any(), anyString(), any());
    }

    @Test
    void capturePayment_success() {
        UUID id = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setStatus(PaymentStatus.AUTHORIZED);
        tx.setExternalTransactionId("ext-999");
        tx.setAmount(new BigDecimal("100"));

        when(repository.findById(id)).thenReturn(Optional.of(tx));
        when(gateway.capture("ext-999", tx.getAmount()))
                .thenReturn(new GatewayResponse(PaymentStatus.CAPTURED, "ext-999", null));
        when(repository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction updated = service.capturePayment(id);

        assertEquals(PaymentStatus.CAPTURED, updated.getStatus());
        verify(gateway, times(1)).capture("ext-999", tx.getAmount());
    }

    @Test
    void capturePayment_invalidStatus_throws() {
        UUID id = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setStatus(PaymentStatus.CAPTURED);
        when(repository.findById(id)).thenReturn(Optional.of(tx));

        assertThrows(RuntimeException.class, () -> service.capturePayment(id));
    }

    @Test
    void refundPayment_success() {
        UUID id = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setStatus(PaymentStatus.CAPTURED);
        tx.setExternalTransactionId("ext-555");
        tx.setAmount(new BigDecimal("20"));

        when(repository.findById(id)).thenReturn(Optional.of(tx));
        when(gateway.refund("ext-555", tx.getAmount())).thenReturn(new GatewayResponse(PaymentStatus.REFUNDED, "ext-555", null));
        when(repository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction updated = service.refundPayment(id);
        assertEquals(PaymentStatus.REFUNDED, updated.getStatus());
        verify(gateway, times(1)).refund("ext-555", tx.getAmount());
    }

    @Test
    void refundPayment_invalidStatus_throws() {
        UUID id = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setStatus(PaymentStatus.AUTHORIZED);
        when(repository.findById(id)).thenReturn(Optional.of(tx));

        assertThrows(RuntimeException.class, () -> service.refundPayment(id));
    }

    @Test
    void handlePaymentIntentSucceeded_updatesStatus() {
        Transaction tx = new Transaction();
        tx.setStatus(PaymentStatus.AUTHORIZED);
        tx.setExternalTransactionId("pi_ok");
        when(repository.findByExternalTransactionId("pi_ok")).thenReturn(Optional.of(tx));
        service.handlePaymentIntentSucceeded("pi_ok");
        assertEquals(PaymentStatus.CAPTURED, tx.getStatus());
        verify(repository, times(1)).save(tx);
    }

    @Test
    void handlePaymentIntentSucceeded_noOpIfAlreadyCaptured() {
        Transaction tx = new Transaction();
        tx.setStatus(PaymentStatus.CAPTURED);
        tx.setExternalTransactionId("pi_already");
        when(repository.findByExternalTransactionId("pi_already")).thenReturn(Optional.of(tx));
        service.handlePaymentIntentSucceeded("pi_already");
        verify(repository, times(0)).save(any(Transaction.class));
    }

    @Test
    void handlePaymentIntentSucceeded_notFound_throws() {
        when(repository.findByExternalTransactionId("missing")).thenReturn(Optional.empty());
        assertThrows(com.ahy.payment.exception.PaymentNotFoundException.class, () -> service.handlePaymentIntentSucceeded("missing"));
    }

    @Test
    void handlePaymentIntentFailed_updatesStatus() {
        Transaction tx = new Transaction();
        tx.setStatus(PaymentStatus.AUTHORIZED);
        tx.setExternalTransactionId("pi_fail");
        when(repository.findByExternalTransactionId("pi_fail")).thenReturn(Optional.of(tx));
        service.handlePaymentIntentFailed("pi_fail");
        assertEquals(PaymentStatus.FAILED, tx.getStatus());
        verify(repository, times(1)).save(tx);
    }

    @Test
    void handlePaymentIntentFailed_noOpIfAlreadyFailed() {
        Transaction tx = new Transaction();
        tx.setStatus(PaymentStatus.FAILED);
        tx.setExternalTransactionId("pi_already_failed");
        when(repository.findByExternalTransactionId("pi_already_failed")).thenReturn(Optional.of(tx));
        service.handlePaymentIntentFailed("pi_already_failed");
        verify(repository, times(0)).save(any(Transaction.class));
    }

    @Test
    void handleChargeRefunded_updatesStatus() {
        Transaction tx = new Transaction();
        tx.setStatus(PaymentStatus.CAPTURED);
        tx.setExternalTransactionId("pi_ref");
        when(repository.findByExternalTransactionId("pi_ref")).thenReturn(Optional.of(tx));
        service.handleChargeRefunded("pi_ref");
        assertEquals(PaymentStatus.REFUNDED, tx.getStatus());
        verify(repository, times(1)).save(tx);
    }

    @Test
    void handleChargeRefunded_noOpIfAlreadyRefunded() {
        Transaction tx = new Transaction();
        tx.setStatus(PaymentStatus.REFUNDED);
        tx.setExternalTransactionId("pi_already_ref");
        when(repository.findByExternalTransactionId("pi_already_ref")).thenReturn(Optional.of(tx));
        service.handleChargeRefunded("pi_already_ref");
        verify(repository, times(0)).save(any(Transaction.class));
    }
}

