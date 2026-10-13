package com.notification_service.services;

import com.notification_service.events.TransactionStatusEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/*
    transaction-service -> "transaction-events"
    Simdilik bildirimler loglanir. E-posta gondermek icin event'e alici e-postasi eklenmeli
    ve spring.mail ayarlari yapilmalidir.
 */
@Component
@Slf4j
public class NotificationConsumer {

    @KafkaListener(topics = "${app.kafka.topics.transaction-events}")
    public void onTransactionEvent(TransactionStatusEvent event) {
        if ("COMPLETED".equals(event.status())) {
            log.info("[NOTIFY {}] Ihre Überweisung über {} € an {} wurde ausgeführt. (Transaktion {})",
                    event.fromAccountNumber(), event.amount(), event.toAccountNumber(), event.transactionId());
            log.info("[NOTIFY {}] Sie haben {} € von {} erhalten. (Transaktion {})",
                    event.toAccountNumber(), event.amount(), event.fromAccountNumber(), event.transactionId());
        } else {
            log.info("[NOTIFY {}] Ihre Überweisung über {} € an {} ist fehlgeschlagen: {} (Transaktion {})",
                    event.fromAccountNumber(), event.amount(), event.toAccountNumber(), event.reason(), event.transactionId());
        }
    }
}
