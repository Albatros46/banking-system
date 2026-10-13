package com.fraud_detection_service.services;

import com.fraud_detection_service.config.FraudProperties;
import com.fraud_detection_service.events.FraudAlertEvent;
import com.fraud_detection_service.events.FraudCheckResultEvent;
import com.fraud_detection_service.events.TransactionInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/*
    SAGA Step 2/3 - transaction-initiated -> kontrol -> fraud-check-results
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {

    private static final String VELOCITY_KEY_PREFIX = "fraud:velocity:";

    private final FraudProperties fraudProperties;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.fraud-check-results}")
    private String fraudCheckResultsTopic;
    @Value("${app.kafka.topics.fraud-alerts}")
    private String fraudAlertsTopic;

    @KafkaListener(topics = "${app.kafka.topics.transaction-initiated}")
    public void onTransactionInitiated(TransactionInitiatedEvent event) {
        log.info("Checking transaction {} ({} -> {}, {})", event.transactionId(),
                event.fromAccountNumber(), event.toAccountNumber(), event.amount());

        FraudCheckResultEvent result;
        if (event.amount().compareTo(fraudProperties.maxSingleAmount()) > 0) {
            result = new FraudCheckResultEvent(event.transactionId(), true,
                    "Betrag überschreitet das Limit pro Überweisung von " + fraudProperties.maxSingleAmount() + " €");
        } else if (isVelocityExceeded(event.fromAccountNumber())) {
            result = new FraudCheckResultEvent(event.transactionId(), true,
                    "Zu viele Überweisungen innerhalb von " + fraudProperties.velocityWindow().toSeconds() + " Sekunden");
            // Kisa surede cok sayida transfer -> hesap ele gecirilmis olabilir, bloke et
            kafkaTemplate.send(fraudAlertsTopic, event.fromAccountNumber(),
                    new FraudAlertEvent(event.transactionId(), event.fromAccountNumber(), result.reason()));
        } else {
            result = new FraudCheckResultEvent(event.transactionId(), false, null);
        }

        if (result.fraudulent()) {
            log.warn("Transaction {} flagged : {}", event.transactionId(), result.reason());
        }
        // Sonuc gonderilemezse exception -> event tekrar islenir (aksi halde transfer PENDING'de kalir)
        kafkaTemplate.send(fraudCheckResultsTopic, event.transactionId(), result).join();
    }

    // Redis sayaci: ilk transferde pencere suresi kadar TTL atanir, sure dolunca sayac sifirlanir
    private boolean isVelocityExceeded(String accountNumber) {
        String key = VELOCITY_KEY_PREFIX + accountNumber;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, fraudProperties.velocityWindow());
        }
        return count != null && count > fraudProperties.maxTransfersPerWindow();
    }
}
