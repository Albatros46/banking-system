package com.fraud_detection_service.events;

/*
    "fraud-alerts" -> account-service (gonderen hesabi bloke eder)
 */
public record FraudAlertEvent(
        String transactionId,
        String accountNumber,
        String reason
) {
}
