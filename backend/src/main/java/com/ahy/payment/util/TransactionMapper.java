package com.ahy.payment.util;

import com.ahy.payment.dto.response.TransactionResponse;
import com.ahy.payment.model.Transaction;

public class TransactionMapper {

    public static TransactionResponse toResponse(Transaction tx) {
        TransactionResponse res = new TransactionResponse();
        res.setId(tx.getId());
        res.setAmount(tx.getAmount());
        res.setCurrency(tx.getCurrency());
        res.setStatus(tx.getStatus());
        res.setCreatedAt(tx.getCreatedAt());
        return res;
    }
}

