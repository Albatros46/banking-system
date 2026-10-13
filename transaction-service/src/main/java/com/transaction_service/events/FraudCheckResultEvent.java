package com.transaction_service.events;

/*
    fraud-detection-service -> "fraud-check-results" -> transaction-service
 */
public record FraudCheckResultEvent(
        String transactionId,
        boolean fraudulent,
        String reason
) {
}
