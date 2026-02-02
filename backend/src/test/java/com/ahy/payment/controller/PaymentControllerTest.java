package com.ahy.payment.controller;

import com.ahy.payment.dto.request.AuthorizeRequest;
import com.ahy.payment.model.PaymentStatus;
import com.ahy.payment.model.Transaction;
import com.ahy.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@org.springframework.context.annotation.Import(com.ahy.payment.exception.GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private com.ahy.payment.util.StripeWebhookVerifier stripeWebhookVerifier;

    @Test
    void authorizePayment_shouldReturn201_andTransaction() throws Exception {
        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());
        tx.setAmount(BigDecimal.valueOf(50));
        tx.setCurrency("USD");
        tx.setStatus(PaymentStatus.AUTHORIZED);

        when(paymentService.authorizePayment(
                any(), any(), any(), any()
        )).thenReturn(tx);

        AuthorizeRequest request = new AuthorizeRequest();
        request.setAmount(BigDecimal.valueOf(50));
        request.setCurrency("USD");
        request.setIdempotencyKey("idem-123");
        request.setMetadata(Map.of("orderId", "ORD-1"));

        mockMvc.perform(post("/payments/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(50))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));
    }

    @org.junit.jupiter.api.Test
    void getPayment_found_shouldReturn200_andTransaction() throws Exception {
        UUID id = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setAmount(BigDecimal.valueOf(10));
        tx.setCurrency("EUR");
        tx.setStatus(PaymentStatus.AUTHORIZED);

        when(paymentService.getTransaction(id)).thenReturn(tx);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/payments/{id}", id))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.currency").value("EUR"));
    }

    @org.junit.jupiter.api.Test
    void getPayment_notFound_shouldReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentService.getTransaction(id)).thenReturn(null);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/payments/{id}", id))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
    }
}
