package com.fraud_detection_service.events;

import java.math.BigDecimal;

/*
    transaction-service -> "transaction-initiated"
 */
public record TransactionInitiatedEvent(
        String transactionId,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount
) {
}
