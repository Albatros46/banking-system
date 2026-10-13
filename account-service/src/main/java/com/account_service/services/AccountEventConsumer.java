package com.account_service.services;

import com.account_service.events.FraudAlertEvent;
import com.account_service.exceptions.AccountNotFoundException;
import com.account_service.exceptions.InvalidAccountOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/*
    fraud-detection-service -> "fraud-alerts"
    Supheli islem tespit edilince gonderen hesap bloke edilir.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AccountEventConsumer {

    private final AccountService accountService;

    @KafkaListener(topics = "${app.kafka.topics.fraud-alerts}")
    public void onFraudAlert(FraudAlertEvent event) {
        log.warn("Fraud alert for account : {} (transaction : {}, reason : {})",
                event.accountNumber(), event.transactionId(), event.reason());
        try {
            accountService.blockAccount(event.accountNumber());
        } catch (AccountNotFoundException | InvalidAccountOperationException ex) {
            // Tekrar denemek sonucu degistirmez, sadece logla
            log.error("Could not block account {} : {}", event.accountNumber(), ex.getMessage());
        }
    }
}
