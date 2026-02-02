package com.ahy.payment;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentServiceApplicationTest {

    @Test
    void has_public_static_main_method() throws Exception {
        Method m = PaymentServiceApplication.class.getMethod("main", String[].class);
        int mods = m.getModifiers();
        assertThat(Modifier.isPublic(mods)).isTrue();
        assertThat(Modifier.isStatic(mods)).isTrue();
    }
}