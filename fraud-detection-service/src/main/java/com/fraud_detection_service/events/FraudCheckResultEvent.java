package com.fraud_detection_service.events;

/*
    "fraud-check-results" -> transaction-service
 */
public record FraudCheckResultEvent(
        String transactionId,
        boolean fraudulent,
        String reason
) {
}
