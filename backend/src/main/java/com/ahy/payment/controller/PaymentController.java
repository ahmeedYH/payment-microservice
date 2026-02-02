package com.ahy.payment.controller;

import com.ahy.payment.dto.request.AuthorizeRequest;
import com.ahy.payment.dto.response.TransactionResponse;
import com.ahy.payment.exception.TransactionNotFoundException;
import com.ahy.payment.model.Transaction;
import com.ahy.payment.service.PaymentService;
import com.ahy.payment.util.SignatureUtil;
import com.ahy.payment.util.TransactionMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final com.ahy.payment.util.StripeWebhookVerifier webhookVerifier;

    public PaymentController(PaymentService paymentService, com.ahy.payment.util.StripeWebhookVerifier webhookVerifier) {
        this.paymentService = paymentService;
        this.webhookVerifier = webhookVerifier;
    }

    @Value("${payments.stripe.webhookSecret}")
    private String webhookSecret;

    // ------------------------------
    // Authorize Payment
    // ------------------------------
    @PostMapping("/authorize")
    public ResponseEntity<TransactionResponse> authorizePayment(@Valid @RequestBody AuthorizeRequest request) {
        Transaction tx = paymentService.authorizePayment(
                request.getAmount(),
                request.getCurrency(),
                request.getMetadata(),
                request.getIdempotencyKey()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionMapper.toResponse(tx));
    }

    // ------------------------------
    // Get Payment
    // ------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getPayment(@PathVariable UUID id) {
        Transaction tx = paymentService.getTransaction(id);
        if (tx == null) throw new TransactionNotFoundException(id);
        return ResponseEntity.ok(TransactionMapper.toResponse(tx));
    }

    // ------------------------------
    // Capture Payment
    // ------------------------------
    @PostMapping("/{id}/capture")
    public ResponseEntity<TransactionResponse> capturePayment(@PathVariable UUID id) {
        Transaction tx = paymentService.capturePayment(id);
        return ResponseEntity.ok(TransactionMapper.toResponse(tx));
    }

    // ------------------------------
    // Refund Payment
    // ------------------------------
    @PostMapping("/{id}/refund")
    public ResponseEntity<TransactionResponse> refundPayment(@PathVariable UUID id) {
        Transaction tx = paymentService.refundPayment(id);
        return ResponseEntity.ok(TransactionMapper.toResponse(tx));
    }

    // ================================
    // Stripe Webhook Endpoint
    // ================================
    @PostMapping("/webhooks/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature
    ) {
        com.ahy.payment.util.WebhookEvent event;

        try {
            event = webhookVerifier.verifyStripeSignature(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().build();
        }

        switch (event.getType()) {

            case "payment_intent.succeeded" -> paymentService.handlePaymentIntentSucceeded(event.getPaymentIntentId());

            case "payment_intent.payment_failed" -> paymentService.handlePaymentIntentFailed(event.getPaymentIntentId());

            case "charge.refunded" -> paymentService.handleChargeRefunded(event.getPaymentIntentId());

            default -> {
                // Ignore unhandled events
            }
        }

        return ResponseEntity.ok().build();
    }

}
