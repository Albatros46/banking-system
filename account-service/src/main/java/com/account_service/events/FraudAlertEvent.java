package com.account_service.events;

/*
    fraud-detection-service -> "fraud-alerts" topic
 */
public record FraudAlertEvent(
        String transactionId,
        String accountNumber,
        String reason
) {
}
