package com.ahy.payment.util;

public class WebhookEvent {
    private final String type;
    private final String paymentIntentId;

    public WebhookEvent(String type, String paymentIntentId) {
        this.type = type;
        this.paymentIntentId = paymentIntentId;
    }

    public String getType() {
        return type;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }
}
