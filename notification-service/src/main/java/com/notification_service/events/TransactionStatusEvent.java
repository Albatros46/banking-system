package com.notification_service.events;

import java.math.BigDecimal;

/*
    transaction-service -> "transaction-events"
 */
public record TransactionStatusEvent(
        String transactionId,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount,
        String status,
        String reason
) {
}
