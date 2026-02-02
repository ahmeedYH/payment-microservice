package com.ahy.payment.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TransactionMapperTest {

    @Test
    void constructor_smoke() {
        TransactionMapper m = new TransactionMapper();
        assertThat(m).isNotNull();
    }

}