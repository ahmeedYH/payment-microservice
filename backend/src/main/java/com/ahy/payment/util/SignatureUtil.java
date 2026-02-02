package com.ahy.payment.util;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

public class SignatureUtil {

    public static Event verifyStripeSignature(
            String payload,
            String signatureHeader,
            String secret
    ) throws SignatureVerificationException {

        return Webhook.constructEvent(
                payload,
                signatureHeader,
                secret
        );
    }
}
