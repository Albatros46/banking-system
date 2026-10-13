package com.fraud_detection_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

/*
    maxSingleAmount     -> tek islemde bu tutari asan transfer reddedilir
    maxTransfers/window -> ayni hesaptan bu sure icinde bu sayidan fazla transfer: reddedilir ve hesap bloke edilir
 */
@ConfigurationProperties(prefix = "app.fraud")
public record FraudProperties(
        BigDecimal maxSingleAmount,
        int maxTransfersPerWindow,
        Duration velocityWindow
) {
}
