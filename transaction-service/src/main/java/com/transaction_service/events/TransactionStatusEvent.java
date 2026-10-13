package com.transaction_service.events;

import java.math.BigDecimal;

/*
    transaction-service -> "transaction-events" -> notification-service
    Transfer sonuclandiginda (COMPLETED / FAILED) yayinlanir.
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
