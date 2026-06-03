package com.maang.reconciliation.consumer.domain;

import java.math.BigDecimal;

public record Transaction(
        String transactionId,
        String accountId,
        BigDecimal amount,
        String source,
        long timestamp
) {}