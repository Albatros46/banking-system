package com.transaction_service.events;

import java.math.BigDecimal;

/*
    transaction-service -> "transaction-initiated" -> fraud-detection-service
 */
public record TransactionInitiatedEvent(
        String transactionId,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount
) {
}
