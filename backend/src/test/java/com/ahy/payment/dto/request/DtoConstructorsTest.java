package com.ahy.payment.dto.request;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DtoConstructorsTest {

    @Test
    void captureRequest_constructor_smoke() {
        CaptureRequest r = new CaptureRequest();
        assertThat(r).isNotNull();
    }

    @Test
    void refundRequest_constructor_smoke() {
        RefundRequest r = new RefundRequest();
        assertThat(r).isNotNull();
    }
}