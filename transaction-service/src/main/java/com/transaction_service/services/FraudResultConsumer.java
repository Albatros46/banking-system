package com.transaction_service.services;

import com.transaction_service.events.FraudCheckResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/*
    fraud-detection-service -> "fraud-check-results"
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FraudResultConsumer {

    private final TransactionService transactionService;

    @KafkaListener(topics = "${app.kafka.topics.fraud-check-results}")
    public void onFraudResult(FraudCheckResultEvent result) {
        log.info("Fraud result received for transaction {} : fraudulent={}", result.transactionId(), result.fraudulent());
        transactionService.handleFraudResult(result);
    }
}
