package com.ahy.payment.gateway;

public interface WebhookGateway {

    Object parseAndVerify(String payload, String signature);
}